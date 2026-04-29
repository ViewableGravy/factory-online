package com.factoryonline.client.bootstrap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.factoryonline.client.scheduler.ClientTickSynchronizer;
import com.factoryonline.foundation.config.RuntimeTiming;
import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.foundation.ids.SimulationIds;
import com.factoryonline.foundation.scheduler.TickScheduler;
import com.factoryonline.foundation.timing.TickControl;
import com.factoryonline.foundation.timing.Ticker;
import com.factoryonline.server.bootstrap.BatchedSimulationRunner;
import com.factoryonline.server.bootstrap.TerminalUiState;
import com.factoryonline.simulation.Simulation;
import com.factoryonline.simulation.SimulationAugmentation;
import com.factoryonline.simulation.SimulationRegistry;
import com.factoryonline.transport.ClientTransport;
import com.factoryonline.transport.commands.AckCommand;
import com.factoryonline.transport.commands.InitialSimulationStateCommand;
import com.factoryonline.transport.commands.JoinSimulationCommand;
import com.factoryonline.transport.commands.RejectionCommand;
import com.factoryonline.transport.commands.SimulationInputCommand;
import com.factoryonline.transport.commands.SimulationUpdateCommand;
import com.factoryonline.transport.commands.TickSyncCommand;

public final class ClientApplication {
    // The long-term target is an extra client delay budget on top of observed network delay.
    // TCP tick sync does not currently carry enough clock information to measure that delay safely.
    // The manual harness still works in whole ticks, so the current value of 4 remains the local safety buffer.
    private static final TerminalUiState TERMINAL_UI_STATE = TerminalUiState.getInstance();

    private final ClientId clientId;
    private final SimulationId requestedSimulationId;
    private final ClientTransport transport;
    private final ClientTickSynchronizer tickSynchronizer;
    private final SimulationRegistry simulationRegistry = new SimulationRegistry();
    private final Map<SimulationId, Map<Integer, SimulationAugmentation>> queuedActionsBySimulation = new HashMap<>();
    private final Map<SimulationId, Map<Integer, Integer>> queuedChecksumsBySimulation = new HashMap<>();
    private final Set<SimulationId> attachedSimulationIds = new HashSet<>();
    private TickScheduler tickScheduler;
    private BatchedSimulationRunner runner;
    private boolean joinRequested;
    private SimulationId joinedSimulationId;

    public ClientApplication(ClientId clientId, SimulationId requestedSimulationId, ClientTransport transport) {
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.requestedSimulationId = Objects.requireNonNull(requestedSimulationId, "requestedSimulationId");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.tickSynchronizer = new ClientTickSynchronizer(clientId);
    }

    public void setup() {
        if (joinRequested) {
            return;
        }

        transport.send(new JoinSimulationCommand(requestedSimulationId), false);
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
        scheduleTicks(true);
        simulateCurrentTick();
    }

    public void scheduleTicks(boolean automaticTickDue) {
        if (tickScheduler == null || runner == null) {
            return;
        }

        int requestedTicks = tickSynchronizer.requestTicks(
            joinedSimulationId,
            tickScheduler.getTick(),
            transport.getCurrentTick(),
            automaticTickDue
        );
        
        tickScheduler.queueTicks(requestedTicks);
    }

    public TickControl getTickControl() {
        return tickSynchronizer.getTickControl();
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

    public int getLocalSimulationTick() {
        if (tickScheduler == null) {
            return -1;
        }

        return tickScheduler.getTick();
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
        transport.send(new SimulationInputCommand(simulationId, validatedAugmentation), true);

        System.out.println(
            "Client " + TERMINAL_UI_STATE.formatClient(clientId)
                + " sent input request for " + TERMINAL_UI_STATE.formatSimulation(simulationId)
                + " on transport tick " + transport.getCurrentTick());
    }

    public void simulateCurrentTick() {
        if (tickScheduler == null || runner == null) {
            return;
        }

        tickScheduler.runQueuedTicks();
    }

    public void cleanup() {
        if (tickScheduler != null) {
            tickScheduler.shutdown();
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
        for (InitialSimulationStateCommand initialState : transport.drainAs(InitialSimulationStateCommand.class)) {
            attachSimulation(initialState);
        }
    }

    private void receiveTickSyncMessages() {
        for (TickSyncCommand tickSyncMessage : transport.drainAs(TickSyncCommand.class)) {
            tickSynchronizer.receive(tickSyncMessage, transport.getCurrentTick());
            queueChecksum(tickSyncMessage);
        }
    }

    private void receiveAcknowledgements() {
        for (AckCommand ackMessage : transport.drainAs(AckCommand.class)) {
            System.out.println(
                "Client " + TERMINAL_UI_STATE.formatClient(clientId)
                    + " received Ack for " + TERMINAL_UI_STATE.formatSimulation(ackMessage.simulationId)
                    + " at tick " + ackMessage.tick + ": " + ackMessage.message);
        }
    }

    private void receiveRejections() {
        for (RejectionCommand rejectionMessage : transport.drainAs(RejectionCommand.class)) {
            System.out.println(
                "Client " + TERMINAL_UI_STATE.formatClient(clientId)
                    + " received Rej for " + TERMINAL_UI_STATE.formatSimulation(rejectionMessage.simulationId)
                    + " at tick " + rejectionMessage.tick + ": " + rejectionMessage.message);
        }
    }

    private synchronized void receiveSimulationUpdates() {
        for (SimulationUpdateCommand simulationUpdate : transport.drainAs(SimulationUpdateCommand.class)) {
            queuedActionsBySimulation
                .computeIfAbsent(simulationUpdate.simulationId, ignored -> new HashMap<>())
                .put(simulationUpdate.tick, simulationUpdate.augmentation);
            System.out.println(
                "Client " + TERMINAL_UI_STATE.formatClient(clientId)
                    + " queued update for tick " + simulationUpdate.tick
                    + " on " + TERMINAL_UI_STATE.formatSimulation(simulationUpdate.simulationId));
        }
    }

    private void attachSimulation(InitialSimulationStateCommand initialState) {
        if (attachedSimulationIds.contains(initialState.simulationId)) {
            return;
        }

        if (tickScheduler == null || runner == null) {
            runner = new BatchedSimulationRunner(1, "client");
            tickScheduler = new TickScheduler(new Ticker(initialState.tick), this::runSimulationTick);
            tickScheduler.setStartupBufferTicks(Math.max(0, RuntimeTiming.CLIENT_TARGET_LOCAL_BUFFER_TICKS - 1));
        }

        Simulation bufferedSimulation = new Simulation(initialState.simulationId, initialState.snapshot);
        simulationRegistry.register(bufferedSimulation);
        attachedSimulationIds.add(initialState.simulationId);
        joinedSimulationId = initialState.simulationId;
        runner.addSimulation(bufferedSimulation);

        System.out.println(
            "Client " + TERMINAL_UI_STATE.formatClient(clientId)
                + " attached " + TERMINAL_UI_STATE.formatSimulation(bufferedSimulation.getId())
                + " at snapshot tick " + initialState.tick
                + " with startup buffer " + RuntimeTiming.CLIENT_TARGET_LOCAL_BUFFER_TICKS);
    }

    private void queueChecksum(TickSyncCommand tickSyncMessage) {
        SimulationId simulationId = tickSyncMessage.simulationId;
        if (joinedSimulationId == null || !joinedSimulationId.equals(simulationId)) {
            return;
        }

        if (tickScheduler != null && tickScheduler.getTick() > tickSyncMessage.serverTick) {
            System.out.println(
                "Client " + TERMINAL_UI_STATE.formatClient(clientId)
                    + " skipped late checksum for tick " + tickSyncMessage.serverTick
                    + " on " + TERMINAL_UI_STATE.formatSimulation(simulationId)
                    + " because local simulation is already at tick " + tickScheduler.getTick());
            return;
        }

        queuedChecksumsBySimulation
            .computeIfAbsent(simulationId, ignored -> new HashMap<>())
            .put(tickSyncMessage.serverTick, tickSyncMessage.serverChecksum);
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

    private void runSimulationTick(int simulationTick) {
        applyQueuedActions(simulationTick);
        runner.runTick(simulationTick);
        compareQueuedChecksum(simulationTick);
    }
}
