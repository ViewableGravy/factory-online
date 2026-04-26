package com.factoryonline.client.bootstrap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.foundation.ids.SimulationIds;
import com.factoryonline.foundation.protocol.AckMessage;
import com.factoryonline.foundation.protocol.AckMessageDTO;
import com.factoryonline.foundation.protocol.InitialSimulationState;
import com.factoryonline.foundation.protocol.InitialSimulationStateDTO;
import com.factoryonline.foundation.protocol.JoinSimulationRequestDTO;
import com.factoryonline.foundation.protocol.RejectionMessage;
import com.factoryonline.foundation.protocol.RejectionMessageDTO;
import com.factoryonline.foundation.protocol.SimulationInputRequestDTO;
import com.factoryonline.foundation.protocol.SimulationUpdate;
import com.factoryonline.foundation.protocol.SimulationUpdateDTO;
import com.factoryonline.foundation.protocol.TickSyncMessage;
import com.factoryonline.foundation.protocol.TickSyncMessageDTO;
import com.factoryonline.server.bootstrap.BatchedSimulationRunner;
import com.factoryonline.server.bootstrap.CustomUserInput;
import com.factoryonline.server.bootstrap.TerminalUiState;
import com.factoryonline.server.bootstrap.Ticker;
import com.factoryonline.simulation.Simulation;
import com.factoryonline.simulation.SimulationAugmentation;
import com.factoryonline.simulation.SimulationRegistry;
import com.factoryonline.transport.ClientTransport;

public final class ClientApplication {
    // The long-term target is 100-200ms of extra client delay on top of observed network delay.
    // The manual harness still works in whole ticks, so the current value of 4 is that local safety buffer.
    private static final String SNAPSHOT_COMMAND = "/snapshot";
    private static final int CLIENT_TARGET_LOCAL_BUFFER_TICKS = 4;
    private static final int CLIENT_LAG_TOLERANCE_TICKS = 2;
    private static final int CLIENT_CATCH_UP_TICKS = 2;
    private static final TerminalUiState TERMINAL_UI_STATE = TerminalUiState.getInstance();

    private final ClientId clientId;
    private final SimulationId requestedSimulationId;
    private final ClientTransport transport;
    private final SimulationRegistry simulationRegistry = new SimulationRegistry();
    private final Map<SimulationId, Map<Integer, SimulationAugmentation>> queuedActionsBySimulation = new HashMap<>();
    private final Map<SimulationId, TickSyncState> tickSyncStatesBySimulation = new HashMap<>();
    private final Set<SimulationId> attachedSimulationIds = new HashSet<>();
    private Ticker ticker;
    private BatchedSimulationRunner runner;
    private int pendingSimulationStartTick = -1;
    private int pendingSimulationSteps;
    private int remainingStartupBufferTicks;
    private boolean joinRequested;
    private SimulationId joinedSimulationId;

    public ClientApplication(ClientId clientId, SimulationId requestedSimulationId, ClientTransport transport) {
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.requestedSimulationId = Objects.requireNonNull(requestedSimulationId, "requestedSimulationId");
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    public void setup() {
        if (joinRequested) {
            return;
        }

        transport.send(new JoinSimulationRequestDTO(requestedSimulationId), false);
        joinRequested = true;
        System.out.println(
            "Client " + TERMINAL_UI_STATE.formatClient(clientId)
                + " requested join for " + formatRequestedSimulation());
    }

    public void processIncomingMessages() {
        receiveInitialStates();
        receiveTickSyncMessages();
        receiveAcknowledgements();
        receiveRejections();
        receiveSimulationUpdates();
    }

    public void tick() {
        processIncomingMessages();
        advanceTick();
        simulateCurrentTick();
    }

    public void advanceTick() {
        if (ticker == null || runner == null) {
            return;
        }

        pendingSimulationStartTick = -1;
        pendingSimulationSteps = 0;

        if (remainingStartupBufferTicks > 0) {
            remainingStartupBufferTicks -= 1;
            return;
        }

        int simulationSteps = determineSimulationSteps();
        for (int index = 0; index < simulationSteps; index += 1) {
            int simulationTick = ticker.tick();
            if (pendingSimulationStartTick < 0) {
                pendingSimulationStartTick = simulationTick;
            }

            pendingSimulationSteps += 1;
        }
    }

    public void handleInput(CustomUserInput userInput) {
        Objects.requireNonNull(userInput, "userInput");

        if (SNAPSHOT_COMMAND.equalsIgnoreCase(userInput.getRaw().strip())) {
            if (runner == null) {
                System.out.println(
                    "Client " + TERMINAL_UI_STATE.formatClient(clientId) + " is still waiting for an initial snapshot");
                return;
            }

            runner.requestSnapshot();
            return;
        }

        SimulationAugmentation augmentation = toAugmentation(userInput);
        if (augmentation == null) {
            return;
        }

        SimulationId simulationId = joinedSimulationId;
        if (simulationId == null) {
            System.out.println("Client " + TERMINAL_UI_STATE.formatClient(clientId) + " is still waiting for an initial snapshot");
            return;
        }

        transport.send(new SimulationInputRequestDTO(simulationId, augmentation), true);

        System.out.println(
            "Client " + TERMINAL_UI_STATE.formatClient(clientId)
                + " sent input request for " + TERMINAL_UI_STATE.formatSimulation(simulationId)
                + " on transport tick " + transport.getCurrentTick());
    }

    public void simulateCurrentTick() {
        if (ticker == null || runner == null || pendingSimulationSteps <= 0 || pendingSimulationStartTick <= 0) {
            return;
        }

        for (int step = 0; step < pendingSimulationSteps; step += 1) {
            int simulationTick = pendingSimulationStartTick + step;
            applyQueuedActions(simulationTick);
            runner.runTick(simulationTick);
        }

        pendingSimulationStartTick = -1;
        pendingSimulationSteps = 0;
    }

    public void cleanup() {
        if (ticker != null) {
            ticker.shutdown();
        }

        if (runner != null) {
            runner.close();
        }
    }

    private synchronized void applyQueuedActions(int tick) {
        for (var entry : queuedActionsBySimulation.entrySet()) {
            if (!attachedSimulationIds.contains(entry.getKey())) {
                continue;
            }

            SimulationAugmentation augmentation = entry.getValue().remove(tick);
            if (augmentation == null) {
                continue;
            }

            Simulation simulation = simulationRegistry.get(entry.getKey());
            simulation.applyAction(augmentation);
        }
    }

    private void receiveInitialStates() {
        for (InitialSimulationState initialState : transport.drainAs(InitialSimulationStateDTO.class)) {
            attachSimulation(initialState);
        }
    }

    private void receiveTickSyncMessages() {
        for (TickSyncMessage tickSyncMessage : transport.drainAs(TickSyncMessageDTO.class)) {
            int observedTransportDelayTicks = Math.max(0, transport.getCurrentTick() - tickSyncMessage.getServerTick());
            tickSyncStatesBySimulation.put(
                tickSyncMessage.getSimulationId(),
                new TickSyncState(tickSyncMessage.getServerTick(), transport.getCurrentTick(), observedTransportDelayTicks));
            System.out.println(
                "Client " + TERMINAL_UI_STATE.formatClient(clientId)
                    + " synced to server tick " + tickSyncMessage.getServerTick()
                    + " for " + TERMINAL_UI_STATE.formatSimulation(tickSyncMessage.getSimulationId())
                    + " with observed delay " + observedTransportDelayTicks + " ticks");
        }
    }

    private void receiveAcknowledgements() {
        for (AckMessage ackMessage : transport.drainAs(AckMessageDTO.class)) {
            System.out.println(
                "Client " + TERMINAL_UI_STATE.formatClient(clientId)
                    + " received Ack for " + TERMINAL_UI_STATE.formatSimulation(ackMessage.getSimulationId())
                    + " at tick " + ackMessage.getTick() + ": " + ackMessage.getMessage());
        }
    }

    private void receiveRejections() {
        for (RejectionMessage rejectionMessage : transport.drainAs(RejectionMessageDTO.class)) {
            System.out.println(
                "Client " + TERMINAL_UI_STATE.formatClient(clientId)
                    + " received Rej for " + TERMINAL_UI_STATE.formatSimulation(rejectionMessage.getSimulationId())
                    + " at tick " + rejectionMessage.getTick() + ": " + rejectionMessage.getMessage());
        }
    }

    private synchronized void receiveSimulationUpdates() {
        for (SimulationUpdate simulationUpdate : transport.drainAs(SimulationUpdateDTO.class)) {
            queuedActionsBySimulation
                .computeIfAbsent(simulationUpdate.getSimulationId(), ignored -> new HashMap<>())
                .put(simulationUpdate.getTick(), simulationUpdate.getAugmentation());
            System.out.println(
                "Client " + TERMINAL_UI_STATE.formatClient(clientId)
                    + " queued update for tick " + simulationUpdate.getTick()
                    + " on " + TERMINAL_UI_STATE.formatSimulation(simulationUpdate.getSimulationId()));
        }
    }

    private void attachSimulation(InitialSimulationState initialState) {
        if (attachedSimulationIds.contains(initialState.getSimulationId())) {
            return;
        }

        if (ticker == null || runner == null) {
            ticker = new Ticker(initialState.getTick());
            runner = new BatchedSimulationRunner(1, "client");
            remainingStartupBufferTicks = Math.max(0, CLIENT_TARGET_LOCAL_BUFFER_TICKS - 1);
        }

        Simulation bufferedSimulation = new Simulation(initialState.getSimulationId(), initialState.getSnapshot());
        simulationRegistry.register(bufferedSimulation);
        attachedSimulationIds.add(initialState.getSimulationId());
        joinedSimulationId = initialState.getSimulationId();
        runner.addSimulation(bufferedSimulation);

        System.out.println(
            "Client " + TERMINAL_UI_STATE.formatClient(clientId)
                + " attached " + TERMINAL_UI_STATE.formatSimulation(bufferedSimulation.getId())
                + " at snapshot tick " + initialState.getTick()
                + " with startup buffer " + CLIENT_TARGET_LOCAL_BUFFER_TICKS);
    }

    private int determineSimulationSteps() {
        SimulationId activeSimulationId = joinedSimulationId;
        if (activeSimulationId == null) {
            return 1;
        }

        TickSyncState tickSyncState = tickSyncStatesBySimulation.get(activeSimulationId);
        if (tickSyncState == null) {
            return 1;
        }

        int estimatedServerTick = tickSyncState.estimateCurrentServerTick(transport.getCurrentTick());
        int targetLagTicks = tickSyncState.observedTransportDelayTicks + CLIENT_TARGET_LOCAL_BUFFER_TICKS;
        int currentLagTicks = estimatedServerTick - ticker.getTick();

        if (currentLagTicks < targetLagTicks - CLIENT_LAG_TOLERANCE_TICKS) {
            return 0;
        }

        if (currentLagTicks > targetLagTicks + CLIENT_LAG_TOLERANCE_TICKS) {
            return CLIENT_CATCH_UP_TICKS;
        }

        return 1;
    }

    private String formatRequestedSimulation() {
        if (SimulationIds.RANDOM.equals(requestedSimulationId)) {
            return "a random server simulation";
        }

        return TERMINAL_UI_STATE.formatSimulation(requestedSimulationId);
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

    private static final class TickSyncState {
        private final int serverTick;
        private final int observedAtTransportTick;
        private final int observedTransportDelayTicks;

        private TickSyncState(int serverTick, int observedAtTransportTick, int observedTransportDelayTicks) {
            this.serverTick = serverTick;
            this.observedAtTransportTick = observedAtTransportTick;
            this.observedTransportDelayTicks = observedTransportDelayTicks;
        }

        private int estimateCurrentServerTick(int currentTransportTick) {
            return serverTick + Math.max(0, currentTransportTick - observedAtTransportTick);
        }
    }
}