package com.factoryonline.server.bootstrap;

import java.util.List;

import com.factoryonline.client.bootstrap.ClientApplication;
import com.factoryonline.simulation.Simulation;
import com.factoryonline.simulation.SimulationActionResult;
import com.factoryonline.simulation.SimulationAugmentation;
import com.factoryonline.simulation.SimulationRegistry;

public final class ServerApplication {
    private ClientApplication client;
    private Ticker ticker;
    private BatchedSimulationRunner runner;
    private SimulationRegistry registry;

    public ServerApplication(ClientApplication client) {
        this.ticker = new Ticker();
        this.runner = new BatchedSimulationRunner(ticker, 2);
        this.registry = new SimulationRegistry();
        this.client = client;
    }

    public void setup() {
        registry.register(new Simulation("Simulation 1"));
        registry.register(new Simulation("Simulation 2"));

        List<Simulation> originalSimulations = registry.all();
        for (Simulation simulation : originalSimulations) {
            runner.addSimulation(simulation);
        }

        client.attachSimulation(registry.get("Simulation 1"));
        Broadcaster.subscribe("Simulation 1", client);

        // increment 5 ticks at beginning for testing buffered client
        runner.awaitCompletion(ticker.tick());
        runner.awaitCompletion(ticker.tick());
        runner.awaitCompletion(ticker.tick());
        runner.awaitCompletion(ticker.tick());
        runner.awaitCompletion(ticker.tick());
    }

    public void run(CustomUserInput userInput) {
        int targetTick = ticker.getTick() + 1;

        if (userInput.isIncrement() || userInput.isDecrement()) {
            Simulation simulation = registry.get("Simulation 1");
            SimulationAugmentation augmentation = userInput.isIncrement()
                ? new SimulationAugmentation(1)
                : new SimulationAugmentation(-1);

            SimulationActionResult result = simulation.applyAction(augmentation);
            if (!result.isSuccess()) {
                System.out.println("Server action rejected: " + result.getError());
            } else {
                Broadcaster.broadcast(simulation.getName(), augmentation, targetTick);
                System.out.println("Broadcasted action for tick " + targetTick + " on " + simulation.getName());
            }
        }

        int currentTick = ticker.tick();
        runner.awaitCompletion(currentTick);
    }

    public void cleanup() {
        runner.close();
    }
}
