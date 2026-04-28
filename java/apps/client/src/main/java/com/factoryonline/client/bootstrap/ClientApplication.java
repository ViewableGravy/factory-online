package com.factoryonline.client.bootstrap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.factoryonline.foundation.config.RuntimeTiming;
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
import com.factoryonline.foundation.timing.TickControl;
import com.factoryonline.server.bootstrap.BatchedSimulationRunner;
import com.factoryonline.server.bootstrap.TerminalUiState;
import com.factoryonline.server.bootstrap.Ticker;
import com.factoryonline.simulation.Simulation;
import com.factoryonline.simulation.SimulationAugmentation;
import com.factoryonline.simulation.SimulationRegistry;
import com.factoryonline.transport.ClientTransport;

public final class ClientApplication {
    // The long-term target is an extra client delay budget on top of observed network delay.
    // The manual harness still works in whole ticks, so the current value of 4 remains the local safety buffer.
    private static final TerminalUiState TERMINAL_UI_STATE = TerminalUiState.getInstance();

    private final ClientId clientId;
    private final SimulationId requestedSimulationId;
    private final ClientTransport transport;
    private final SimulationRegistry simulationRegistry = new SimulationRegistry();
    private final Map<SimulationId, Map<Integer, SimulationAugmentation>> queuedActionsBySimulation = new HashMap<>();
    private final Map<SimulationId, Map<Integer, Integer>> queuedChecksumsBySimulation = new HashMap<>();
    private final Map<SimulationId, TickSyncState> tickSyncStatesBySimulation = new HashMap<>();
    private final Set<SimulationId> attachedSimulationIds = new HashSet<>();
    private TickControl tickControl = TickControl.automatic(RuntimeTiming.TICK_INTERVAL_MILLIS);
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

    public TickControl getTickControl() {
        return tickControl;
    }

    public boolean canRequestSnapshot() {
        return runner != null;
    }

    public boolean hasJoinedSimulation() {
        return joinedSimulationId != null;
    }

    public String getFormattedClientLabel() {
        return TERMINAL_UI_STATE.formatClient(clientId);
    }

    public void requestSnapshot() {
        if (runner == null) {
            throw new IllegalStateException("snapshot requires an attached simulation");
        }

        runner.requestSnapshot();
    }

    public void requestSimulationInput(SimulationAugmentation augmentation) {
        SimulationId simulationId = joinedSimulationId;
        if (simulationId == null) {
            throw new IllegalStateException("simulation input requires a joined simulation");
        }

        SimulationAugmentation validatedAugmentation = Objects.requireNonNull(augmentation, "augmentation");
        transport.send(new SimulationInputRequestDTO(simulationId, validatedAugmentation), true);

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
            compareQueuedChecksum(simulationTick);
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
            TickSyncState previousState = tickSyncStatesBySimulation.get(tickSyncMessage.getSimulationId());
            double pacingAdjustmentCredit = previousState == null ? 0.0D : previousState.pacingAdjustmentCredit;
            
            tickSyncStatesBySimulation.put(
                tickSyncMessage.getSimulationId(),
                new TickSyncState(
                    tickSyncMessage.getServerTick(),
                    transport.getCurrentTick(),
                    observedTransportDelayTicks,
                    pacingAdjustmentCredit,
                    tickSyncMessage.getTickControl()
                )
            );
            tickControl = tickSyncMessage.getTickControl();
            queueChecksum(tickSyncMessage);
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
            remainingStartupBufferTicks = Math.max(0, RuntimeTiming.CLIENT_TARGET_LOCAL_BUFFER_TICKS - 1);
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
                + " with startup buffer " + RuntimeTiming.CLIENT_TARGET_LOCAL_BUFFER_TICKS);
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

        if (tickSyncState.tickControl.isManual()) {
            return Math.max(0, tickSyncState.serverTick - ticker.getTick());
        }

        int estimatedServerTick = tickSyncState.estimateCurrentServerTick(transport.getCurrentTick());
        int targetLagTicks = tickSyncState.observedTransportDelayTicks + RuntimeTiming.CLIENT_TARGET_LOCAL_BUFFER_TICKS;
        int currentLagTicks = estimatedServerTick - ticker.getTick();
        int lagErrorTicks = currentLagTicks - targetLagTicks;

        if (lagErrorTicks < -RuntimeTiming.CLIENT_HARD_CORRECTION_TICKS) {
            System.out.println(
                "Client " + TERMINAL_UI_STATE.formatClient(clientId)
                    + " is ahead of the server for " + TERMINAL_UI_STATE.formatSimulation(activeSimulationId)
                    + " (current lag " + currentLagTicks + " ticks, target " + targetLagTicks
                    + " +/- " + RuntimeTiming.CLIENT_HARD_CORRECTION_TICKS + "); holding local simulation");
            return 0;
        }

        if (lagErrorTicks > RuntimeTiming.CLIENT_HARD_CORRECTION_TICKS) {
            System.out.println(
                "Client " + TERMINAL_UI_STATE.formatClient(clientId)
                    + " is behind the server for " + TERMINAL_UI_STATE.formatSimulation(activeSimulationId)
                    + " (current lag " + currentLagTicks + " ticks, target " + targetLagTicks
                    + " +/- " + RuntimeTiming.CLIENT_HARD_CORRECTION_TICKS + "); running catch-up ticks");
            return RuntimeTiming.CLIENT_CATCH_UP_TICKS;
        }

        tickSyncState.pacingAdjustmentCredit += lagErrorTicks * RuntimeTiming.CLIENT_RATE_ADJUSTMENT_GAIN;
        if (tickSyncState.pacingAdjustmentCredit <= -1.0D
            && lagErrorTicks < -RuntimeTiming.CLIENT_LAG_TOLERANCE_TICKS) {
            tickSyncState.pacingAdjustmentCredit += 1.0D;
            return 0;
        }

        if (tickSyncState.pacingAdjustmentCredit >= 1.0D
            && lagErrorTicks > RuntimeTiming.CLIENT_LAG_TOLERANCE_TICKS) {
            tickSyncState.pacingAdjustmentCredit -= 1.0D;
            return RuntimeTiming.CLIENT_CATCH_UP_TICKS;
        }

        return 1;
    }

    private void queueChecksum(TickSyncMessage tickSyncMessage) {
        SimulationId simulationId = tickSyncMessage.getSimulationId();
        if (joinedSimulationId == null || !joinedSimulationId.equals(simulationId)) {
            return;
        }

        if (ticker != null && ticker.getTick() > tickSyncMessage.getServerTick()) {
            System.out.println(
                "Client " + TERMINAL_UI_STATE.formatClient(clientId)
                    + " skipped late checksum for tick " + tickSyncMessage.getServerTick()
                    + " on " + TERMINAL_UI_STATE.formatSimulation(simulationId)
                    + " because local simulation is already at tick " + ticker.getTick());
            return;
        }

        queuedChecksumsBySimulation
            .computeIfAbsent(simulationId, ignored -> new HashMap<>())
            .put(tickSyncMessage.getServerTick(), tickSyncMessage.getServerChecksum());
    }

    private void compareQueuedChecksum(int tick) {
        SimulationId activeSimulationId = joinedSimulationId;
        if (activeSimulationId == null) {
            return;
        }

        Map<Integer, Integer> queuedChecksums = queuedChecksumsBySimulation.get(activeSimulationId);
        if (queuedChecksums == null) {
            return;
        }

        Integer expectedChecksum = queuedChecksums.remove(tick);
        if (expectedChecksum == null) {
            return;
        }

        Simulation simulation = simulationRegistry.get(activeSimulationId);
        int localChecksum = simulation.checksum();
        if (localChecksum == expectedChecksum.intValue()) {
            return;
        }

        System.out.println(
            "Client " + TERMINAL_UI_STATE.formatClient(clientId)
                + " checksum mismatch at tick " + tick
                + " on " + TERMINAL_UI_STATE.formatSimulation(activeSimulationId)
                + " (local " + localChecksum + ", server " + expectedChecksum + ")");
    }

    private String formatRequestedSimulation() {
        if (SimulationIds.RANDOM.equals(requestedSimulationId)) {
            return "a random server simulation";
        }

        return TERMINAL_UI_STATE.formatSimulation(requestedSimulationId);
    }

    private static final class TickSyncState {
        private final int serverTick;
        private final int observedAtTransportTick;
        private final int observedTransportDelayTicks;
        private final TickControl tickControl;
        private double pacingAdjustmentCredit;

        private TickSyncState(
            int serverTick,
            int observedAtTransportTick,
            int observedTransportDelayTicks,
            double pacingAdjustmentCredit,
            TickControl tickControl
        ) {
            this.serverTick = serverTick;
            this.observedAtTransportTick = observedAtTransportTick;
            this.observedTransportDelayTicks = observedTransportDelayTicks;
            this.pacingAdjustmentCredit = pacingAdjustmentCredit;
            this.tickControl = Objects.requireNonNull(tickControl, "tickControl");
        }

        private int estimateCurrentServerTick(int currentTransportTick) {
            return serverTick + Math.max(0, currentTransportTick - observedAtTransportTick);
        }
    }
}
