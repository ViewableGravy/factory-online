package com.factoryonline.server.bootstrap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.simulation.Simulation;
import com.factoryonline.simulation.SimulationActionResult;
import com.factoryonline.simulation.SimulationAugmentation;
import com.factoryonline.simulation.SimulationRegistry;
import com.factoryonline.transport.local.JoinSimulationRequest;
import com.factoryonline.transport.local.LocalServerTransport;

public final class ServerApplication {
    private static final SimulationId PRIMARY_SIMULATION_ID = new SimulationId("Simulation 1");

    private final LocalServerTransport transport;
    private final Ticker ticker;
    private final BatchedSimulationRunner runner;
    private final SimulationRegistry registry;
    private final Broadcaster broadcaster;
    private final Map<ClientId, SimulationId> simulationIdsByClientId = new HashMap<>();

    public ServerApplication(LocalServerTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");

        this.ticker = new Ticker();
        this.runner = new BatchedSimulationRunner(ticker, 2, "server");
        this.registry = new SimulationRegistry();
        this.broadcaster = new Broadcaster(transport);
    }

    public void setup() {
        registerSimulation(new Simulation(PRIMARY_SIMULATION_ID));
        registerSimulation(new Simulation(new SimulationId("Simulation 2")));
    }

    public void tick(CustomUserInput userInput) {
        handleJoinRequests();

        int targetTick = ticker.getTick() + 1;

        if (userInput.isIncrement() || userInput.isDecrement()) {
            Simulation simulation = registry.get(PRIMARY_SIMULATION_ID);
            SimulationAugmentation augmentation = userInput.isIncrement()
                ? new SimulationAugmentation(1)
                : new SimulationAugmentation(-1);

            SimulationActionResult result = simulation.applyAction(augmentation);
            if (!result.isSuccess()) {
                System.out.println("Server action rejected: " + result.getError());
            } else {
                broadcaster.broadcast(PRIMARY_SIMULATION_ID, targetTick, augmentation);

                System.out.println("Broadcasted action for tick " + targetTick + " on " + simulation.getId());
            }
        }

        advanceSimulationTick();
    }

    public void cleanup() {
        runner.close();
    }

    public void handleJoinRequests() {
        for (JoinSimulationRequest joinRequest : transport.drainJoinRequests()) {
            ClientId clientId = joinRequest.getClientId();
            if (simulationIdsByClientId.containsKey(clientId)) {
                System.out.println("Server ignored duplicate join from " + clientId);
                continue;
            }

            SimulationId simulationId = joinRequest.getSimulationId();
            Simulation simulation = registry.getOrNull(simulationId);
            if (simulation == null) {
                System.out.println("Server rejected join from " + clientId + ": " + simulationId + " not found");
                continue;
            }

            simulationIdsByClientId.put(clientId, simulationId);
            broadcaster.subscribe(simulationId, clientId);
            transport.sendInitialState(clientId, simulationId, simulation.snapshot(), ticker.getTick());
            
            System.out.println(
                "Server accepted join from " + clientId
                    + " for " + simulationId
                    + " at current server tick " + ticker.getTick());
        }
    }

    private void registerSimulation(Simulation simulation) {
        registry.register(simulation);
        runner.addSimulation(simulation);
    }

    private void advanceSimulationTick() {
        int currentTick = ticker.tick();
        runner.awaitCompletion(currentTick);
    }
}
