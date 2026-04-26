package com.factoryonline.foundation.protocol;

import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.simulation.SimulationAugmentation;

public final class SimulationInputRequest {
    private final SimulationId simulationId;
    private final SimulationAugmentation augmentation;

    public SimulationInputRequest(SimulationId simulationId, SimulationAugmentation augmentation) {
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        this.augmentation = Objects.requireNonNull(augmentation, "augmentation");
    }

    public SimulationId getSimulationId() {
        return simulationId;
    }

    public SimulationAugmentation getAugmentation() {
        return augmentation;
    }
}