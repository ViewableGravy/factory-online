package com.factoryonline.server.bootstrap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import com.factoryonline.simulation.Simulation;
import com.factoryonline.simulation.SimulationRegistry;

public final class ServerApplication {
    private static final int WORKER_COUNT = 4;
    private static final int BUFFER_DELAY_TICKS = 5;

    private ServerApplication() {
    }

    public static void run(String[] args) throws IOException {
        Ticker primaryTicker = new Ticker();
        Ticker bufferedTicker = new Ticker();
        BatchedSimulationRunner primaryRunner = new BatchedSimulationRunner(primaryTicker, WORKER_COUNT);
        BatchedSimulationRunner bufferedRunner = new BatchedSimulationRunner(bufferedTicker, WORKER_COUNT);
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

            List<Simulation> originalSimulations = simulationRegistry.all();
            for (Simulation simulation : originalSimulations) {
                primaryRunner.addSimulation(simulation);
            }

            for (Simulation simulation : originalSimulations) {
                bufferedRunner.addSimulation(new Simulation(simulation.getName() + " Buffered", simulation.snapshot()));
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter text and press Enter: ");
            String userInput;

            while ((userInput = reader.readLine()) != null) {
                System.out.println("\nServer received: " + userInput);

                int currentTick = primaryTicker.tick();
                primaryRunner.awaitCompletion(currentTick);

                // Only run buffered simulation after a delay to ensure that they run with a defined delay
                if (currentTick > BUFFER_DELAY_TICKS) {
                    int bufferedTick = bufferedTicker.tick();
                    bufferedRunner.awaitCompletion(bufferedTick);
                }

                System.out.print("Enter text and press Enter: ");
            }
        } finally {
            bufferedRunner.close();
            primaryRunner.close();
        }
    }
}
