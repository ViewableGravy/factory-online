package com.factoryonline.server.bootstrap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.simulation.Simulation;
import com.factoryonline.simulation.SimulationActionResult;
import com.factoryonline.simulation.SimulationRegistry;
import com.factoryonline.transport.local.JoinSimulationRequest;
import com.factoryonline.transport.local.LocalServerTransport;
import com.factoryonline.transport.local.SimulationInputRequest;

public final class ServerApplication {
    private static final String ADD_SIMULATION_COMMAND = "/add-simulation";
    private static final String SIMULATION_NAME_PREFIX = "Simulation ";
    private static final SimulationId PRIMARY_SIMULATION_ID = new SimulationId("Simulation 1");

    private final LocalServerTransport transport;
    private final Ticker ticker;
    private final BatchedSimulationRunner runner;
    private final SimulationRegistry registry;
    private final Broadcaster broadcaster;
    private final Map<ClientId, SimulationId> simulationIdsByClientId = new HashMap<>();
    private int pendingSimulationTick = -1;

    public ServerApplication(LocalServerTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");

        this.ticker = new Ticker();
        this.runner = new BatchedSimulationRunner(2, "server");
        this.registry = new SimulationRegistry();
        this.broadcaster = new Broadcaster(transport);
    }

    public void setup() {
        registerSimulation(new Simulation(PRIMARY_SIMULATION_ID));
        registerSimulation(new Simulation(new SimulationId("Simulation 2")));
    }

    public void processIncomingMessages() {
        handleJoinRequests();
        handleSimulationInputRequests();
    }

    public void advanceTick() {
        pendingSimulationTick = ticker.tick();
    }

    public void handleAdminCommand(String rawCommand) {
        String normalizedCommand = Objects.requireNonNull(rawCommand, "rawCommand").strip();

        if (!ADD_SIMULATION_COMMAND.equalsIgnoreCase(normalizedCommand)) {
            System.out.println("Server ignored unknown command: " + normalizedCommand);
            return;
        }

        Simulation simulation = new Simulation(createNextSimulationId());
        registerSimulation(simulation);
        System.out.println("Server added simulation " + simulation.getId() + " in base state");
    }

    public void simulateCurrentTick() {
        if (pendingSimulationTick <= 0) {
            return;
        }

        runner.runTick(pendingSimulationTick);
        pendingSimulationTick = -1;
    }

    public void cleanup() {
        ticker.shutdown();
        runner.close();
    }

    private void handleJoinRequests() {
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

    private void handleSimulationInputRequests() {
        for (SimulationInputRequest inputRequest : transport.drainSimulationInputRequests()) {
            Simulation simulation = registry.get(inputRequest.getSimulationId());
            SimulationActionResult result = simulation.applyAction(inputRequest.getAugmentation());
            if (!result.isSuccess()) {
                System.out.println("Server action rejected: " + result.getError());
                continue;
            }

            int targetTick = ticker.getTick();
            broadcaster.broadcast(inputRequest.getSimulationId(), targetTick, inputRequest.getAugmentation());

            System.out.println(
                "Server applied input from " + inputRequest.getClientId()
                    + " for tick " + targetTick
                    + " on " + simulation.getId());
        }
    }

    private void registerSimulation(Simulation simulation) {
        registry.register(simulation);
        runner.addSimulation(simulation);
    }

    private SimulationId createNextSimulationId() {
        int simulationNumber = registry.all().size() + 1;
        SimulationId candidateId = new SimulationId(SIMULATION_NAME_PREFIX + simulationNumber);

        while (registry.getOrNull(candidateId) != null) {
            simulationNumber += 1;
            candidateId = new SimulationId(SIMULATION_NAME_PREFIX + simulationNumber);
        }

        return candidateId;
    }
}
