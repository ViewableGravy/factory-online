package com.factoryonline.server.bootstrap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.factoryonline.simulation.Simulation;
import com.factoryonline.simulation.SimulationRegistry;
import com.factoryonline.simulation.SimulationSnapshot;

public final class ServerApplication {
    private ServerApplication() {
    }

    public static void run(String[] args) throws IOException {
        Ticker ticker = new Ticker();
        BatchedSimulationRunner runner = new BatchedSimulationRunner(ticker);
        SimulationRegistry simulationRegistry = new SimulationRegistry();

        try {
            simulationRegistry.register(new Simulation("Simulation 1"));
            simulationRegistry.register(new Simulation("Simulation 2"));
            simulationRegistry.register(new Simulation("Simulation 3"));
            simulationRegistry.register(new Simulation("Simulation 4"));
            simulationRegistry.register(new Simulation("Simulation 5"));
            simulationRegistry.register(new Simulation("Simulation 6"));
            simulationRegistry.register(new Simulation("Simulation 7"));
            simulationRegistry.register(new Simulation("Simulation 8"));

            for (Simulation simulation : simulationRegistry.all()) {
                runner.addSimulation(simulation);
            }

            /***** SNAPSHOT CLONE CHECK *****/
            int firstTick = ticker.tick();
            runner.awaitCompletion(firstTick);

            Simulation originalSimulation = simulationRegistry.get("Simulation 1");
            SimulationSnapshot originalSnapshot = originalSimulation.snapshot();
            Simulation clonedSimulation = new Simulation("Simulation 1 Clone", originalSnapshot);

            simulationRegistry.register(clonedSimulation);
            runner.addSimulation(clonedSimulation);

            for (int tick = 0; tick < 5; tick++) {
                int comparisonTick = ticker.tick();
                runner.awaitCompletion(comparisonTick);
                
                ensureSnapshotsMatch(originalSimulation.snapshot(), clonedSimulation.snapshot());
            }

            System.out.println("Snapshot clone check passed: " + originalSimulation.getName());

            /***** APPLICATION LOOP *****/

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter text and press Enter: ");
            String userInput;
            while ((userInput = reader.readLine()) != null) {
                System.out.println("Server received: " + userInput);
                int currentTick = ticker.tick();
                runner.awaitCompletion(currentTick);
                System.out.print("Enter text and press Enter: ");
            }
        } finally {
            runner.close();
        }
    }

    private static void ensureSnapshotsMatch(SimulationSnapshot expected, SimulationSnapshot actual) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(
                "Simulation snapshots do not match: expected " + expected + ", actual " + actual);
        }
    }
}