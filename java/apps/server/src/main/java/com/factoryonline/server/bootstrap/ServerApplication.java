package com.factoryonline.server.bootstrap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.factoryonline.foundation.config.RuntimeTiming;
import com.factoryonline.foundation.config.TerminalCommands;
import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.foundation.ids.SimulationIds;
import com.factoryonline.foundation.timing.TickControl;
import com.factoryonline.simulation.Simulation;
import com.factoryonline.simulation.SimulationActionResult;
import com.factoryonline.simulation.SimulationAugmentation;
import com.factoryonline.simulation.SimulationRegistry;
import com.factoryonline.simulation.tick.Ticker;
import com.factoryonline.transport.ServerTransport;
import com.factoryonline.transport.commands.AckCommand;
import com.factoryonline.transport.commands.AuthRequestCommand;
import com.factoryonline.transport.commands.AuthSuccessCommand;
import com.factoryonline.transport.commands.ClientTransportCommand;
import com.factoryonline.transport.commands.InitialSimulationStateCommand;
import com.factoryonline.transport.commands.JoinSimulationCommand;
import com.factoryonline.transport.commands.ProtocolCommand;
import com.factoryonline.transport.commands.RejectionCommand;
import com.factoryonline.transport.commands.SimulationInputCommand;
import com.factoryonline.transport.commands.TickSyncCommand;

public final class ServerApplication {
    private static final TerminalUiState TERMINAL_UI_STATE = TerminalUiState.getInstance();

    private final ServerTransport transport;
    private final Ticker ticker;
    private final ServerTickController tickController;
    private final BatchedSimulationRunner runner;
    private final SimulationRegistry registry;
    private final Broadcaster broadcaster;
    private final SimulationIdFactory simulationIdFactory;
    private final Map<ClientId, String> authTokensByClientId = new HashMap<>();
    private final Map<ClientId, Session> sessionsByClientId = new HashMap<>();
    private final Map<ClientId, String> usernamesByClientId = new HashMap<>();
    private final Map<Integer, List<BufferedSimulationInput>> bufferedInputsByTick = new HashMap<>();
    private final Map<Integer, Map<SimulationId, Simulation>> validationSimulationsByTick = new HashMap<>();

    public ServerApplication(ServerTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.ticker = new Ticker();
        this.tickController = ServerTickController.automatic();
        this.runner = new BatchedSimulationRunner(2, "server");
        this.registry = new SimulationRegistry();
        this.broadcaster = new Broadcaster(this.transport);
        this.simulationIdFactory = new SimulationIdFactory();
    }

    public ServerApplication configureDefault() {
        registerSimulation(new Simulation(simulationIdFactory.create()));
        registerSimulation(new Simulation(simulationIdFactory.create()));
        registerSimulation(new Simulation(simulationIdFactory.create()));
        return this;
    }

    public void processIncomingMessages() {
        for (ClientTransportCommand clientMessage : transport.drainMessages()) {
            dispatchClientMessage(clientMessage);
        }
    }

    public void advanceTick() {
        ticker.tick();
    }

    public synchronized TickControl getTickControl() {
        return tickController.getTickControl();
    }

    public ServerTickController getTickController() {
        return tickController;
    }

    public void cleanup() {
        runner.close();
    }

    public boolean hasActiveSession() {
        return findAnySession() != null;
    }

    public void requestSnapshot() {
        runner.requestSnapshot();
    }

    public void runRegisteredSimulations(long tick) {
        runner.runTick(tick);
    }

    public SimulationId addSimulation() {
        Simulation simulation = new Simulation(simulationIdFactory.create());
        registerSimulation(simulation);
        return simulation.id;
    }

    public void queueServerSimulationInput(SimulationAugmentation augmentation) {
        Session serverSession = findAnySession();
        if (serverSession == null) {
            throw new IllegalStateException("server simulation input requires an active session");
        }

        SimulationAugmentation validatedAugmentation = Objects.requireNonNull(augmentation, "augmentation");
        int targetTick = nextInputTargetTick();
        SimulationActionResult validationResult = queueValidatedInput(serverSession, validatedAugmentation, targetTick);
        if (!validationResult.success) {
            System.out.println(
                TERMINAL_UI_STATE.formatServerLabel()
                    + " rejected "
                    + TerminalCommands.SERVER_COMMAND_PREFIX
                    + " input: "
                    + validationResult.error);
            return;
        }

        broadcaster.broadcast(serverSession.simulationId, targetTick, validatedAugmentation);
        System.out.println(
            TERMINAL_UI_STATE.formatServerLabel()
                + " applied "
                + TerminalCommands.SERVER_COMMAND_PREFIX
                + " input for tick "
                + targetTick
                + " on " + TERMINAL_UI_STATE.formatSimulation(serverSession.simulationId));
    }

    public void broadcastCurrentTickControlState() {
        broadcastCurrentTickState(currentSnapshotTick());
    }

    private void dispatchClientMessage(ClientTransportCommand clientMessage) {
        ClientTransportCommand validatedMessage = Objects.requireNonNull(clientMessage, "clientMessage");
        ProtocolCommand payload = validatedMessage.payload;

        if (payload instanceof AuthRequestCommand) {
            handleAuthRequest(validatedMessage.clientId, (AuthRequestCommand) payload);
            return;
        }

        if (!isAuthenticated(validatedMessage)) {
            reject(validatedMessage.clientId, simulationIdFor(payload), "unauthenticated: missing or invalid token");
            return;
        }

        if (payload instanceof JoinSimulationCommand) {
            handleJoinRequest(validatedMessage.clientId, (JoinSimulationCommand) payload);
            return;
        }

        if (payload instanceof SimulationInputCommand) {
            handleSimulationInputRequest(
                validatedMessage.clientId,
                (SimulationInputCommand) payload);
            return;
        }

        System.out.println(
            TERMINAL_UI_STATE.formatServerLabel()
                + " ignored unknown payload from " + usernameFor(validatedMessage.clientId)
                + ": " + payload.getClass().getName());
    }

    private void handleAuthRequest(ClientId clientId, AuthRequestCommand request) {
        ClientId validatedClientId = Objects.requireNonNull(clientId, "clientId");
        String username = Objects.requireNonNull(request.username, "username");
        String token = UUID.randomUUID().toString();
        authTokensByClientId.put(validatedClientId, token);
        usernamesByClientId.put(validatedClientId, username);
        transport.send(validatedClientId, new AuthSuccessCommand(token));
        System.out.println(
            TERMINAL_UI_STATE.formatServerLabel()
                + " authenticated " + usernameFor(validatedClientId));
    }

    private String usernameFor(ClientId clientId) {
        String name = usernamesByClientId.get(clientId);
        if (name != null) {
            return name;
        }

        return TERMINAL_UI_STATE.formatClient(clientId);
    }

    private void handleJoinRequest(ClientId clientId, JoinSimulationCommand joinRequest) {
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(joinRequest, "joinRequest");

        if (sessionsByClientId.containsKey(clientId)) {
            reject(clientId, joinRequest.simulationId, "join rejected: duplicate session for client");
            return;
        }

        Simulation simulation = resolveRequestedSimulation(joinRequest.simulationId);
        if (simulation == null) {
            reject(clientId, joinRequest.simulationId, "join rejected: simulation not found");
            return;
        }

        SimulationId simulationId = simulation.id;

        Session session = new Session(clientId, simulationId);
        sessionsByClientId.put(clientId, session);

        broadcaster.subscribe(simulationId, clientId);
        int snapshotTick = currentSnapshotTick();
        transport.send(clientId, new InitialSimulationStateCommand(simulationId, simulation.snapshot(), snapshotTick));
        transport.send(clientId, new TickSyncCommand(simulationId, snapshotTick, simulation.checksum(), getTickControl()));
        transport.send(clientId, new AckCommand(simulationId, currentTick(), "join accepted"));

        System.out.println(
            TERMINAL_UI_STATE.formatServerLabel() + " accepted join from " + usernameFor(clientId)
                + " for " + TERMINAL_UI_STATE.formatSimulation(simulationId)
                + " at current server tick " + currentTick());
    }

    private void handleSimulationInputRequest(ClientId clientId, SimulationInputCommand inputRequest) {
        Objects.requireNonNull(clientId, "clientId");
        Objects.requireNonNull(inputRequest, "inputRequest");

        Session session = sessionsByClientId.get(clientId);
        if (session == null) {
            reject(clientId, inputRequest.simulationId, "input rejected: no active session");
            return;
        }

        if (!session.simulationId.equals(inputRequest.simulationId)) {
            reject(clientId, inputRequest.simulationId, "input rejected: simulation does not match session");
            return;
        }

        int targetTick = nextInputTargetTick();
        SimulationActionResult validationResult = queueValidatedInput(session, inputRequest.augmentation, targetTick);
        if (!validationResult.success) {
            reject(clientId, inputRequest.simulationId, "input rejected: " + validationResult.error);
            return;
        }

        transport.send(clientId, new AckCommand(inputRequest.simulationId, targetTick, "input accepted"));
        broadcaster.broadcast(inputRequest.simulationId, targetTick, inputRequest.augmentation);
        System.out.println(
            TERMINAL_UI_STATE.formatServerLabel() + " accepted input from " + usernameFor(clientId)
                + " for tick " + targetTick
                + " on " + TERMINAL_UI_STATE.formatSimulation(session.simulationId));
    }

    private SimulationActionResult queueValidatedInput(Session session, SimulationAugmentation augmentation, int targetTick) {
        Session validatedSession = Objects.requireNonNull(session, "session");
        SimulationAugmentation validatedAugmentation = Objects.requireNonNull(augmentation, "augmentation");

        Simulation validationSimulation = validationSimulationsByTick
            .computeIfAbsent(targetTick, ignored -> new HashMap<>())
            .computeIfAbsent(validatedSession.simulationId, ignored -> {
                Simulation liveSimulation = registry.get(validatedSession.simulationId);
                return new Simulation(liveSimulation.id, liveSimulation.snapshot());
            });
        SimulationActionResult validationResult = validationSimulation.applyAction(validatedAugmentation);
        if (!validationResult.success) {
            return validationResult;
        }

        bufferedInputsByTick
            .computeIfAbsent(targetTick, ignored -> new ArrayList<>())
            .add(new BufferedSimulationInput(validatedSession, validatedAugmentation));
        return SimulationActionResult.success();
    }

    private int nextInputTargetTick() {
        return currentTick() + RuntimeTiming.SERVER_INPUT_LEAD_TICKS;
    }

    public void applyBufferedInputs(long tick) {
        int protocolTick = toProtocolTick(tick);
        List<BufferedSimulationInput> bufferedInputs = bufferedInputsByTick.remove(protocolTick);
        validationSimulationsByTick.remove(protocolTick);
        if (bufferedInputs == null || bufferedInputs.isEmpty()) {
            return;
        }

        for (BufferedSimulationInput bufferedInput : bufferedInputs) {
            Simulation simulation = registry.get(bufferedInput.session.simulationId);
            SimulationActionResult result = simulation.applyAction(bufferedInput.augmentation);
            if (!result.success) {
                throw new IllegalStateException(
                    "Buffered input failed after validation for " + bufferedInput.session.simulationId + ": "
                        + result.error);
            }
        }
    }

    private void reject(ClientId clientId, SimulationId simulationId, String message) {
        transport.send(clientId, new RejectionCommand(simulationId, currentTick(), message));
        System.out.println(
            TERMINAL_UI_STATE.formatServerLabel() + " rejected " + usernameFor(clientId)
                + ": " + message);
    }

    private boolean isAuthenticated(ClientTransportCommand clientMessage) {
        String expectedToken = authTokensByClientId.get(clientMessage.clientId);
        return expectedToken != null && expectedToken.equals(clientMessage.sessionToken);
    }

    private SimulationId simulationIdFor(ProtocolCommand payload) {
        if (payload instanceof JoinSimulationCommand) {
            return ((JoinSimulationCommand) payload).simulationId;
        }

        if (payload instanceof SimulationInputCommand) {
            return ((SimulationInputCommand) payload).simulationId;
        }

        return SimulationIds.RANDOM;
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

    private int currentSnapshotTick() {
        return currentTick();
    }

    private void registerSimulation(Simulation simulation) {
        registry.register(simulation);
        runner.addSimulation(simulation);
    }

    private boolean shouldBroadcastTickSync(int tick) {
        TickControl currentTickControl = getTickControl();
        return currentTickControl.isManual() || tick % RuntimeTiming.SERVER_TICK_SYNC_INTERVAL == 0;
    }

    private void broadcastCurrentTickState(int tick) {
        TickControl currentTickControl = getTickControl();
        for (Simulation simulation : registry.all()) {
            broadcaster.broadcastTickSync(
                simulation.id,
                tick,
                simulation.checksum(),
                currentTickControl);
        }
    }

    public void broadcastCurrentTickStateIfDue(long tick) {
        int simulationTick = toProtocolTick(tick);
        if (shouldBroadcastTickSync(simulationTick)) {
            broadcastCurrentTickState(simulationTick);
        }
    }

    private int currentTick() {
        return toProtocolTick(ticker.getCurrentTick());
    }

    private int toProtocolTick(long tick) {
        if (tick > Integer.MAX_VALUE) {
            throw new IllegalStateException("tick exceeds protocol range: " + tick);
        }

        return (int) tick;
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
