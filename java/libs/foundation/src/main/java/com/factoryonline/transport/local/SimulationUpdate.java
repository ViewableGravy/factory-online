package com.factoryonline.transport.local;

import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.simulation.SimulationAugmentation;

public final class SimulationUpdate {
    private final SimulationId simulationId;
    private final SimulationAugmentation augmentation;
    private final int tick;

    public SimulationUpdate(SimulationId simulationId, SimulationAugmentation augmentation, int tick) {
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        this.augmentation = Objects.requireNonNull(augmentation, "augmentation");

        if (tick <= 0) {
            throw new IllegalArgumentException("tick must be positive");
        }

        this.tick = tick;
    }

    public SimulationId getSimulationId() {
        return simulationId;
    }

    public SimulationAugmentation getAugmentation() {
        return augmentation;
    }

    public int getTick() {
        return tick;
    }
}