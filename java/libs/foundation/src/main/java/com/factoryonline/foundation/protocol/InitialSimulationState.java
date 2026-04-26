package com.factoryonline.foundation.protocol;

import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.simulation.SimulationSnapshot;

public final class InitialSimulationState {
    private final SimulationId simulationId;
    private final SimulationSnapshot snapshot;
    private final int tick;

    public InitialSimulationState(SimulationId simulationId, SimulationSnapshot snapshot, int tick) {
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");

        if (tick < 0) {
            throw new IllegalArgumentException("tick must not be negative");
        }

        this.tick = tick;
    }

    public SimulationId getSimulationId() {
        return simulationId;
    }

    public SimulationSnapshot getSnapshot() {
        return snapshot;
    }

    public int getTick() {
        return tick;
    }
}