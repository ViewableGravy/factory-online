package com.factoryonline.server.bootstrap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class ServerApplication {
    private ServerApplication() {
    }

    public static void run(String[] args) throws IOException {
        Ticker ticker = new Ticker();
        BatchedSimulationRunner runner = new BatchedSimulationRunner(ticker);

        try {
            runner.addSimulation(new Simulation("Simulation 1"));
            runner.addSimulation(new Simulation("Simulation 2"));
            runner.addSimulation(new Simulation("Simulation 3"));
            runner.addSimulation(new Simulation("Simulation 4"));
            runner.addSimulation(new Simulation("Simulation 5"));
            runner.addSimulation(new Simulation("Simulation 6"));
            runner.addSimulation(new Simulation("Simulation 7"));
            runner.addSimulation(new Simulation("Simulation 8"));
            runner.addSimulation(new Simulation("Simulation 9"));
            runner.addSimulation(new Simulation("Simulation 10"));
            runner.addSimulation(new Simulation("Simulation 11"));
            runner.addSimulation(new Simulation("Simulation 12"));
            runner.addSimulation(new Simulation("Simulation 13"));
            runner.addSimulation(new Simulation("Simulation 14"));
            runner.addSimulation(new Simulation("Simulation 15"));
            runner.addSimulation(new Simulation("Simulation 16"));
            runner.addSimulation(new Simulation("Simulation 17"));
            runner.addSimulation(new Simulation("Simulation 18"));
            runner.addSimulation(new Simulation("Simulation 19"));

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
}