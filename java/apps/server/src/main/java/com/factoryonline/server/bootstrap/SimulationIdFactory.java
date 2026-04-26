package com.factoryonline.server.bootstrap;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.factoryonline.foundation.ids.SimulationId;

public final class SimulationIdFactory {
    private static final String DEFAULT_PREFIX = "Simulation ";

    private final String autoPrefix;
    private final Set<SimulationId> reservedIds = new HashSet<>();
    private int nextAutoId = 1;

    public SimulationIdFactory() {
        this(DEFAULT_PREFIX);
    }

    public SimulationIdFactory(String autoPrefix) {
        this.autoPrefix = requireNonBlank(autoPrefix, "autoPrefix");
    }

    public SimulationId create() {
        while (true) {
            SimulationId candidateId = new SimulationId(autoPrefix + nextAutoId);
            nextAutoId += 1;

            if (reservedIds.add(candidateId)) {
                return candidateId;
            }
        }
    }

    public SimulationId create(String simulationId) {
        return create(new SimulationId(simulationId));
    }

    public SimulationId create(SimulationId simulationId) {
        SimulationId validatedSimulationId = Objects.requireNonNull(simulationId, "simulationId");
        if (!reservedIds.add(validatedSimulationId)) {
            throw new IllegalArgumentException("Simulation id already reserved: " + validatedSimulationId);
        }

        updateNextAutoId(validatedSimulationId);
        return validatedSimulationId;
    }

    private void updateNextAutoId(SimulationId simulationId) {
        String value = simulationId.value();
        if (!value.startsWith(autoPrefix)) {
            return;
        }

        String suffix = value.substring(autoPrefix.length());
        if (suffix.isBlank()) {
            return;
        }

        try {
            int parsedValue = Integer.parseInt(suffix);
            if (parsedValue >= nextAutoId) {
                nextAutoId = parsedValue + 1;
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private static String requireNonBlank(String value, String label) {
        String validatedValue = Objects.requireNonNull(value, label);
        if (validatedValue.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }

        return validatedValue;
    }
}