package com.factoryonline.server.bootstrap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.foundation.protocol.JoinSimulationRequest;
import com.factoryonline.foundation.protocol.JoinSimulationRequestDTO;
import com.factoryonline.foundation.protocol.SimulationInputRequest;
import com.factoryonline.foundation.protocol.SimulationInputRequestDTO;
import com.factoryonline.simulation.Simulation;
import com.factoryonline.simulation.SimulationActionResult;
import com.factoryonline.simulation.SimulationRegistry;
import com.factoryonline.transport.local.LocalServerTransport;

public final class ServerApplication {
    private static final String ADD_SIMULATION_COMMAND = "/add-simulation";
    private static final TerminalUiState TERMINAL_UI_STATE = TerminalUiState.getInstance();

    private final LocalServerTransport transport;
    private final Ticker ticker;
    private final BatchedSimulationRunner runner;
    private final SimulationRegistry registry;
    private final Broadcaster broadcaster;
    private final SimulationIdFactory simulationIdFactory;
    private final Map<ClientId, SimulationId> simulationIdsByClientId = new HashMap<>();
    private int pendingSimulationTick = -1;

    public ServerApplication(LocalServerTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");

        this.ticker = new Ticker();
        this.runner = new BatchedSimulationRunner(2, "server");
        this.registry = new SimulationRegistry();
        this.broadcaster = new Broadcaster(transport);
        this.simulationIdFactory = new SimulationIdFactory();
    }

    public void setup() {
        registerSimulation(new Simulation(simulationIdFactory.create()));
        registerSimulation(new Simulation(simulationIdFactory.create()));
        registerSimulation(new Simulation(simulationIdFactory.create()));
    }

    public void processIncomingMessages() {
        handleJoinRequests();
        handleSimulationInputRequests();
    }

    public void advanceTick() {
        pendingSimulationTick = ticker.tick();
    }

    public void simulateCurrentTick() {
        if (pendingSimulationTick <= 0)
            return;

        runner.runTick(pendingSimulationTick);
        pendingSimulationTick = -1;
    }

    public void cleanup() {
        ticker.shutdown();
        runner.close();
    }

    public void handleAdminCommand(String rawCommand) {
        String normalizedCommand = Objects.requireNonNull(rawCommand, "rawCommand").strip();

        if (!ADD_SIMULATION_COMMAND.equalsIgnoreCase(normalizedCommand)) {
            System.out.println("Server ignored unknown command: " + normalizedCommand);
            return;
        }

        Simulation simulation = new Simulation(simulationIdFactory.create());
        registerSimulation(simulation);
        System.out.println(
            TERMINAL_UI_STATE.formatServerLabel()
                + " added simulation " + TERMINAL_UI_STATE.formatSimulation(simulation.getId()) + " in base state");
    }

    private void handleJoinRequests() {
        for (JoinSimulationRequest joinRequest : transport.drainAs(JoinSimulationRequestDTO.class)) {
            ClientId clientId = joinRequest.getClientId();
            if (simulationIdsByClientId.containsKey(clientId)) {
                System.out.println(
                    TERMINAL_UI_STATE.formatServerLabel()
                        + " ignored duplicate join from " + TERMINAL_UI_STATE.formatClient(clientId));
                continue;
            }

            SimulationId simulationId = joinRequest.getSimulationId();
            Simulation simulation = registry.getOrNull(simulationId);
            if (simulation == null) {
                System.out.println(
                    TERMINAL_UI_STATE.formatServerLabel()
                        + " rejected join from " + TERMINAL_UI_STATE.formatClient(clientId)
                        + ": " + TERMINAL_UI_STATE.formatSimulation(simulationId) + " not found");
                continue;
            }

            simulationIdsByClientId.put(clientId, simulationId);
            broadcaster.subscribe(simulationId, clientId);
            transport.sendInitialState(clientId, simulationId, simulation.snapshot(), ticker.getTick());
            
            System.out.println(
                TERMINAL_UI_STATE.formatServerLabel() + " accepted join from " + TERMINAL_UI_STATE.formatClient(clientId)
                    + " for " + TERMINAL_UI_STATE.formatSimulation(simulationId)
                    + " at current server tick " + ticker.getTick());
        }
    }

    private void handleSimulationInputRequests() {
        for (SimulationInputRequest inputRequest : transport.drainAs(SimulationInputRequestDTO.class)) {
            Simulation simulation = registry.get(inputRequest.getSimulationId());
            SimulationActionResult result = simulation.applyAction(inputRequest.getAugmentation());
            if (!result.isSuccess()) {
                System.out.println("Server action rejected: " + result.getError());
                continue;
            }

            int targetTick = ticker.getTick();
            broadcaster.broadcast(inputRequest.getSimulationId(), targetTick, inputRequest.getAugmentation());

            System.out.println(
                TERMINAL_UI_STATE.formatServerLabel() + " applied input from " + TERMINAL_UI_STATE.formatClient(inputRequest.getClientId())
                    + " for tick " + targetTick
                    + " on " + TERMINAL_UI_STATE.formatSimulation(simulation.getId()));
        }
    }

    private void registerSimulation(Simulation simulation) {
        registry.register(simulation);
        runner.addSimulation(simulation);
    }
}
