package com.factoryonline.simulation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SimulationRegistry {
    private final Map<String, Simulation> simulations = new HashMap<>();

    public synchronized void register(Simulation simulation) throws IllegalArgumentException {
        Objects.requireNonNull(simulation, "simulation");

        String name = simulation.getName();
        if (simulations.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate simulation name: " + name);
        }

        simulations.put(name, simulation);
    }

    public synchronized Simulation get(String name) {
        String requestedName = Objects.requireNonNull(name, "name");
        Simulation simulation = simulations.get(requestedName);

        if (simulation == null) {
            throw new IllegalArgumentException("Unknown simulation: " + requestedName);
        }

        return simulation;
    }

    public synchronized List<Simulation> all() {
        return List.copyOf(simulations.values());
    }
}