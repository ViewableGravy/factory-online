package com.factoryonline.simulation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;

public final class SimulationRegistry {
    private final Map<SimulationId, Simulation> simulations = new HashMap<>();

    public synchronized void register(Simulation simulation) throws IllegalArgumentException {
        Objects.requireNonNull(simulation, "simulation");

        SimulationId simulationId = simulation.getId();
        if (simulations.containsKey(simulationId)) {
            throw new IllegalArgumentException("Duplicate simulation name: " + simulationId);
        }

        simulations.put(simulationId, simulation);
    }

    public synchronized Simulation get(SimulationId simulationId) {
        SimulationId requestedSimulationId = Objects.requireNonNull(simulationId, "simulationId");
        Simulation simulation = simulations.get(requestedSimulationId);

        if (simulation == null) {
            throw new IllegalArgumentException("Unknown simulation: " + requestedSimulationId);
        }

        return simulation;
    }

    public synchronized Simulation get(String name) {
        return get(new SimulationId(name));
    }

    public synchronized Simulation getOrNull(SimulationId simulationId) {
        SimulationId requestedSimulationId = Objects.requireNonNull(simulationId, "simulationId");
        return simulations.get(requestedSimulationId);
    }

    public synchronized Simulation getOrNull(String name) {
        return getOrNull(new SimulationId(name));
    }

    public synchronized List<Simulation> all() {
        return List.copyOf(simulations.values());
    }
}