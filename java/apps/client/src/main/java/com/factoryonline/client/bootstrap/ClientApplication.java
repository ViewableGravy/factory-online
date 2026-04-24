package com.factoryonline.client.bootstrap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.factoryonline.server.bootstrap.BatchedSimulationRunner;
import com.factoryonline.server.bootstrap.SimulationClient;
import com.factoryonline.server.bootstrap.Ticker;
import com.factoryonline.simulation.Simulation;
import com.factoryonline.simulation.SimulationAugmentation;
import com.factoryonline.simulation.SimulationRegistry;

public final class ClientApplication implements SimulationClient {
    private final Ticker ticker;
    private final BatchedSimulationRunner runner;
    private final SimulationRegistry simulationRegistry = new SimulationRegistry();
    private final Map<String, Map<Integer, SimulationAugmentation>> queuedActionsBySimulation = new HashMap<>();

    public ClientApplication() {
        this.ticker = new Ticker();
        this.runner = new BatchedSimulationRunner(ticker, 1);
    }

    public void setup() {
    }

    public static void run(String[] args) throws IOException {
        System.out.println("Factory Online client scaffold");
        System.out.println("ClientApplication is now primarily driven by ServerApplication for the in-process prototype.");
    }

    public void attachSimulation(Simulation simulation) {
        Simulation bufferedSimulation = new Simulation(simulation.getName(), simulation.snapshot());
        simulationRegistry.register(bufferedSimulation);
        runner.addSimulation(bufferedSimulation);
    }

    @Override
    public synchronized void receiveBroadcast(String simulationName, SimulationAugmentation augmentation, int tick) {
        queuedActionsBySimulation
            .computeIfAbsent(simulationName, ignored -> new HashMap<>())
            .put(tick, augmentation);
    }

    public void run() {
        int clientTick = ticker.getTick() + 1;
        applyQueuedActions(clientTick);

        int completedTick = ticker.tick();
        runner.awaitCompletion(completedTick);
    }

    public void cleanup() {
        runner.close();
    }

    private synchronized void applyQueuedActions(int tick) {
        for (Map.Entry<String, Map<Integer, SimulationAugmentation>> entry : queuedActionsBySimulation.entrySet()) {
            SimulationAugmentation augmentation = entry.getValue().remove(tick);
            if (augmentation == null) {
                continue;
            }

            Simulation simulation = simulationRegistry.get(entry.getKey());
            simulation.applyAction(augmentation);
        }
    }
}