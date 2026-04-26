package com.factoryonline.foundation.protocol;

import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;

public final class JoinSimulationRequest {
    private final SimulationId simulationId;

    public JoinSimulationRequest(SimulationId simulationId) {
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
    }

    public SimulationId getSimulationId() {
        return simulationId;
    }
}