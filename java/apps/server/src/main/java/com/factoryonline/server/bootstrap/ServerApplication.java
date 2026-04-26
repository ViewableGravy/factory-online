package com.factoryonline.server.bootstrap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.foundation.ids.SimulationIds;
import com.factoryonline.foundation.protocol.AckMessageDTO;
import com.factoryonline.foundation.protocol.ClientTransportMessage;
import com.factoryonline.foundation.protocol.InitialSimulationStateDTO;
import com.factoryonline.foundation.protocol.JoinSimulationRequest;
import com.factoryonline.foundation.protocol.JoinSimulationRequestDTO;
import com.factoryonline.foundation.protocol.ProtocolDTO;
import com.factoryonline.foundation.protocol.ProtocolDTOContainer;
import com.factoryonline.foundation.protocol.RejectionMessageDTO;
import com.factoryonline.foundation.protocol.SimulationInputRequest;
import com.factoryonline.foundation.protocol.SimulationInputRequestDTO;
import com.factoryonline.simulation.Simulation;
import com.factoryonline.simulation.SimulationActionResult;
import com.factoryonline.simulation.SimulationAugmentation;
import com.factoryonline.simulation.SimulationRegistry;
import com.factoryonline.transport.ServerTransport;

public final class ServerApplication {
    private static final String ADD_SIMULATION_COMMAND = "/add-simulation";
    private static final String SNAPSHOT_COMMAND = "/snapshot";
    private static final int TICK_SYNC_INTERVAL = 2;
    private static final TerminalUiState TERMINAL_UI_STATE = TerminalUiState.getInstance();

    private final ServerTransport transport;
    private final Ticker ticker;
    private final BatchedSimulationRunner runner;
    private final SimulationRegistry registry;
    private final Broadcaster broadcaster;
    private final SimulationIdFactory simulationIdFactory;
    private final Map<ClientId, Session> sessionsByClientId = new HashMap<>();
    private final Map<Integer, List<BufferedSimulationInput>> bufferedInputsByTick = new HashMap<>();
    private final Map<Integer, Map<SimulationId, Simulation>> validationSimulationsByTick = new HashMap<>();
    private int pendingSimulationTick = -1;

    public ServerApplication(ServerTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");

        this.ticker = new Ticker();
        this.runner = new BatchedSimulationRunner(2, "server");
        this.registry = new SimulationRegistry();
        this.broadcaster = new Broadcaster(transport);
        this.simulationIdFactory = new SimulationIdFactory();
    }

    public void setup() {
        registerSimulation(new Simulation(simulationIdFactory.create()));
        registerSimulation(new Simulation(simulationIdFactory.create()));
        registerSimulation(new Simulation(simulationIdFactory.create()));
    }

    public void processIncomingMessages() {
        for (ClientTransportMessage clientMessage : transport.drainMessages()) {
            dispatchClientMessage(clientMessage);
        }
    }

    public void advanceTick() {
        pendingSimulationTick = ticker.tick();
    }

    public void simulateCurrentTick() {
        if (pendingSimulationTick <= 0)
            return;

        applyBufferedInputs(pendingSimulationTick);
        runner.runTick(pendingSimulationTick);
        if (pendingSimulationTick % TICK_SYNC_INTERVAL == 0) {
            for (Simulation simulation : registry.all()) {
                broadcaster.broadcastTickSync(
                    simulation.getId(),
                    pendingSimulationTick,
                    simulation.checksum());
            }
        }
        pendingSimulationTick = -1;
    }

    public void cleanup() {
        ticker.shutdown();
        runner.close();
    }

    public void handleAdminCommand(String rawCommand) {
        String normalizedCommand = Objects.requireNonNull(rawCommand, "rawCommand").strip();

        if (SNAPSHOT_COMMAND.equalsIgnoreCase(normalizedCommand)) {
            runner.requestSnapshot();
            return;
        }

        if (!ADD_SIMULATION_COMMAND.equalsIgnoreCase(normalizedCommand)) {
            System.out.println("Server ignored unknown command: " + normalizedCommand);
            return;
        }

        Simulation simulation = new Simulation(simulationIdFactory.create());
        registerSimulation(simulation);
        System.out.println(
            TERMINAL_UI_STATE.formatServerLabel()
                + " added simulation " + TERMINAL_UI_STATE.formatSimulation(simulation.getId()) + " in base state");
    }

    public void handleServerCommand(String rawCommand) {
        CustomUserInput serverInput = CustomUserInput.fromRaw(Objects.requireNonNull(rawCommand, "rawCommand"));
        SimulationAugmentation augmentation = toAugmentation(serverInput);
        if (augmentation == null) {
            System.out.println(
                TERMINAL_UI_STATE.formatServerLabel() + " ignored unknown /server command: " + rawCommand.strip());
            return;
        }

        Session serverSession = findAnySession();
        if (serverSession == null) {
            System.out.println(TERMINAL_UI_STATE.formatServerLabel() + " ignored /server command: no active sessions");
            return;
        }

        int targetTick = ticker.getTick();
        SimulationActionResult validationResult = queueValidatedInput(serverSession, augmentation, targetTick);
        if (!validationResult.isSuccess()) {
            System.out.println(
                TERMINAL_UI_STATE.formatServerLabel() + " rejected /server input: " + validationResult.getError());
            return;
        }

        broadcaster.broadcast(serverSession.getSimulationId(), targetTick, augmentation);
        System.out.println(
            TERMINAL_UI_STATE.formatServerLabel()
                + " applied /server input for tick " + targetTick
                + " on " + TERMINAL_UI_STATE.formatSimulation(serverSession.getSimulationId()));
    }

    private void dispatchClientMessage(ClientTransportMessage clientMessage) {
        ClientTransportMessage validatedMessage = Objects.requireNonNull(clientMessage, "clientMessage");
        ProtocolDTOContainer payload = validatedMessage.getPayload();

        if (JoinSimulationRequestDTO.ID.equals(payload.getId())) {
            handleJoinRequest(validatedMessage.getClientId(), ProtocolDTO.fromContainer(JoinSimulationRequestDTO.class, payload));
            return;
        }

        if (SimulationInputRequestDTO.ID.equals(payload.getId())) {
            handleSimulationInputRequest(
                validatedMessage.getClientId(),
                ProtocolDTO.fromContainer(SimulationInputRequestDTO.class, payload));
            return;
        }

        System.out.println(
            TERMINAL_UI_STATE.formatServerLabel()
                + " ignored unknown payload from " + TERMINAL_UI_STATE.formatClient(validatedMessage.getClientId())
                + ": " + payload.getId().value());
    }

    private void handleJoinRequest(ClientId clientId, JoinSimulationRequest joinRequest) {
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(joinRequest, "joinRequest");

        if (sessionsByClientId.containsKey(clientId)) {
            reject(clientId, joinRequest.getSimulationId(), "join rejected: duplicate session for client");
            return;
        }

        Simulation simulation = resolveRequestedSimulation(joinRequest.getSimulationId());
        if (simulation == null) {
            reject(clientId, joinRequest.getSimulationId(), "join rejected: simulation not found");
            return;
        }

        SimulationId simulationId = simulation.getId();

        Session session = new Session(clientId, simulationId);
        sessionsByClientId.put(clientId, session);

        broadcaster.subscribe(simulationId, clientId);
        int snapshotTick = currentSnapshotTick();
        transport.send(clientId, new InitialSimulationStateDTO(simulationId, simulation.snapshot(), snapshotTick));
        transport.send(clientId, new AckMessageDTO(simulationId, ticker.getTick(), "join accepted"));

        System.out.println(
            TERMINAL_UI_STATE.formatServerLabel() + " accepted join from " + TERMINAL_UI_STATE.formatClient(clientId)
                + " for " + TERMINAL_UI_STATE.formatSimulation(simulationId)
                + " at current server tick " + ticker.getTick());
    }

    private void handleSimulationInputRequest(ClientId clientId, SimulationInputRequest inputRequest) {
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(inputRequest, "inputRequest");

        Session session = sessionsByClientId.get(clientId);
        if (session == null) {
            reject(clientId, inputRequest.getSimulationId(), "input rejected: no active session");
            return;
        }

        if (!session.getSimulationId().equals(inputRequest.getSimulationId())) {
            reject(clientId, inputRequest.getSimulationId(), "input rejected: simulation does not match session");
            return;
        }

        int targetTick = ticker.getTick();
        SimulationActionResult validationResult = queueValidatedInput(session, inputRequest.getAugmentation(), targetTick);
        if (!validationResult.isSuccess()) {
            reject(clientId, inputRequest.getSimulationId(), "input rejected: " + validationResult.getError());
            return;
        }

        transport.send(clientId, new AckMessageDTO(inputRequest.getSimulationId(), targetTick, "input accepted"));
        broadcaster.broadcast(inputRequest.getSimulationId(), targetTick, inputRequest.getAugmentation());
        System.out.println(
            TERMINAL_UI_STATE.formatServerLabel() + " accepted input from " + TERMINAL_UI_STATE.formatClient(clientId)
                + " for tick " + targetTick
                + " on " + TERMINAL_UI_STATE.formatSimulation(session.getSimulationId()));
    }

    private SimulationActionResult queueValidatedInput(Session session, SimulationAugmentation augmentation, int targetTick) {
        Session validatedSession = Objects.requireNonNull(session, "session");
        SimulationAugmentation validatedAugmentation = Objects.requireNonNull(augmentation, "augmentation");

        Simulation validationSimulation = validationSimulationsByTick
            .computeIfAbsent(targetTick, ignored -> new HashMap<>())
            .computeIfAbsent(validatedSession.getSimulationId(), ignored -> {
                Simulation liveSimulation = registry.get(validatedSession.getSimulationId());
                return new Simulation(liveSimulation.getId(), liveSimulation.snapshot());
            });
        SimulationActionResult validationResult = validationSimulation.applyAction(validatedAugmentation);
        if (!validationResult.isSuccess()) {
            return validationResult;
        }

        bufferedInputsByTick
            .computeIfAbsent(targetTick, ignored -> new ArrayList<>())
            .add(new BufferedSimulationInput(validatedSession, validatedAugmentation));
        return SimulationActionResult.success();
    }

    private void applyBufferedInputs(int tick) {
        List<BufferedSimulationInput> bufferedInputs = bufferedInputsByTick.remove(tick);
        validationSimulationsByTick.remove(tick);
        if (bufferedInputs == null || bufferedInputs.isEmpty()) {
            return;
        }

        for (BufferedSimulationInput bufferedInput : bufferedInputs) {
            Simulation simulation = registry.get(bufferedInput.session.getSimulationId());
            SimulationActionResult result = simulation.applyAction(bufferedInput.augmentation);
            if (!result.isSuccess()) {
                throw new IllegalStateException(
                    "Buffered input failed after validation for " + bufferedInput.session.getSimulationId() + ": "
                        + result.getError());
            }
        }
    }

    private void reject(ClientId clientId, SimulationId simulationId, String message) {
        transport.send(clientId, new RejectionMessageDTO(simulationId, ticker.getTick(), message));
        System.out.println(
            TERMINAL_UI_STATE.formatServerLabel() + " rejected " + TERMINAL_UI_STATE.formatClient(clientId)
                + ": " + message);
    }

    private Session findAnySession() {
        for (Session session : sessionsByClientId.values()) {
            return session;
        }

        return null;
    }

    private Simulation resolveRequestedSimulation(SimulationId requestedSimulationId) {
        SimulationId validatedSimulationId = Objects.requireNonNull(requestedSimulationId, "requestedSimulationId");
        if (!SimulationIds.RANDOM.equals(validatedSimulationId)) {
            return registry.getOrNull(validatedSimulationId);
        }

        List<Simulation> simulations = registry.all();
        if (simulations.isEmpty()) {
            return null;
        }

        return simulations.get(ThreadLocalRandom.current().nextInt(simulations.size()));
    }

    private static SimulationAugmentation toAugmentation(CustomUserInput userInput) {
        if (userInput.isIncrement()) {
            return new SimulationAugmentation(1);
        }

        if (userInput.isDecrement()) {
            return new SimulationAugmentation(-1);
        }

        return null;
    }

    private int currentSnapshotTick() {
        if (pendingSimulationTick > 0) {
            return pendingSimulationTick - 1;
        }

        return ticker.getTick();
    }

    private void registerSimulation(Simulation simulation) {
        registry.register(simulation);
        runner.addSimulation(simulation);
    }

    private static final class BufferedSimulationInput {
        private final Session session;
        private final SimulationAugmentation augmentation;

        private BufferedSimulationInput(Session session, SimulationAugmentation augmentation) {
            this.session = Objects.requireNonNull(session, "session");
            this.augmentation = Objects.requireNonNull(augmentation, "augmentation");
        }
    }
}
