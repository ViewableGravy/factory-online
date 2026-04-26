package com.factoryonline.client.bootstrap;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.foundation.protocol.InitialSimulationState;
import com.factoryonline.foundation.protocol.SimulationUpdate;
import com.factoryonline.server.bootstrap.BatchedSimulationRunner;
import com.factoryonline.server.bootstrap.CustomUserInput;
import com.factoryonline.server.bootstrap.Ticker;
import com.factoryonline.simulation.Simulation;
import com.factoryonline.simulation.SimulationAugmentation;
import com.factoryonline.simulation.SimulationRegistry;
import com.factoryonline.transport.local.LocalClientTransport;

public final class ClientApplication {
    private static final int CLIENT_STARTUP_BUFFER_TICKS = 4;

    private final ClientId clientId;
    private final SimulationId requestedSimulationId;
    private final LocalClientTransport transport;
    private final SimulationRegistry simulationRegistry = new SimulationRegistry();
    private final Map<SimulationId, Map<Integer, SimulationAugmentation>> queuedActionsBySimulation = new HashMap<>();
    private final Set<SimulationId> attachedSimulationIds = new HashSet<>();
    private Ticker ticker;
    private BatchedSimulationRunner runner;
    private int pendingSimulationTick = -1;
    private int remainingStartupBufferTicks;
    private boolean joinRequested;

    public ClientApplication(ClientId clientId, SimulationId requestedSimulationId, LocalClientTransport transport) {
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.requestedSimulationId = Objects.requireNonNull(requestedSimulationId, "requestedSimulationId");
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    public void setup() {
        if (joinRequested) {
            return;
        }

        transport.requestJoin(requestedSimulationId);
        joinRequested = true;
        System.out.println("Client " + clientId + " requested join for " + requestedSimulationId);
    }

    public static void run(String[] args) throws IOException {
        System.out.println("Factory Online client scaffold");
        System.out.println("Use the shared transport harness to connect this client to a server endpoint.");
    }

    public void processIncomingMessages() {
        receiveInitialStates();
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

        if (remainingStartupBufferTicks > 0) {
            remainingStartupBufferTicks -= 1;
            return;
        }

        pendingSimulationTick = ticker.tick();
    }

    public void handleInput(CustomUserInput userInput) {
        Objects.requireNonNull(userInput, "userInput");

        SimulationAugmentation augmentation = toAugmentation(userInput);
        if (augmentation == null) {
            return;
        }

        transport.sendSimulationInput(requestedSimulationId, augmentation);

        System.out.println(
            "Client " + clientId
                + " sent input request for " + requestedSimulationId
                + " on transport tick " + transport.getCurrentTick());
    }

    public void simulateCurrentTick() {
        if (ticker == null || runner == null || pendingSimulationTick <= 0) {
            return;
        }

        applyQueuedActions(pendingSimulationTick);
        runner.runTick(pendingSimulationTick);
        pendingSimulationTick = -1;
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
        for (InitialSimulationState initialState : transport.drainInitialStates()) {
            attachSimulation(initialState);
        }
    }

    private synchronized void receiveSimulationUpdates() {
        for (SimulationUpdate simulationUpdate : transport.drainSimulationUpdates()) {
            queuedActionsBySimulation
                .computeIfAbsent(simulationUpdate.getSimulationId(), ignored -> new HashMap<>())
                .put(simulationUpdate.getTick(), simulationUpdate.getAugmentation());
            System.out.println(
                "Client " + clientId
                    + " queued update for tick " + simulationUpdate.getTick()
                    + " on " + simulationUpdate.getSimulationId());
        }
    }

    private void attachSimulation(InitialSimulationState initialState) {
        if (attachedSimulationIds.contains(initialState.getSimulationId())) {
            return;
        }

        if (ticker == null || runner == null) {
            ticker = new Ticker(initialState.getTick());
            runner = new BatchedSimulationRunner(1, "client");
            remainingStartupBufferTicks = Math.max(0, CLIENT_STARTUP_BUFFER_TICKS - 1);
        }

        Simulation bufferedSimulation = new Simulation(initialState.getSimulationId(), initialState.getSnapshot());
        simulationRegistry.register(bufferedSimulation);
        attachedSimulationIds.add(initialState.getSimulationId());
        runner.addSimulation(bufferedSimulation);

        System.out.println(
            "Client " + clientId
                + " attached " + bufferedSimulation.getId()
                + " at snapshot tick " + initialState.getTick()
                + " with startup buffer " + CLIENT_STARTUP_BUFFER_TICKS);
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
}