package com.factoryonline.foundation.protocol;

import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;

public final class RejectionMessage {
    private final SimulationId simulationId;
    private final int tick;
    private final String message;

    public RejectionMessage(SimulationId simulationId, int tick, String message) {
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        this.tick = tick;
        this.message = Objects.requireNonNull(message, "message");
    }

    public SimulationId getSimulationId() {
        return simulationId;
    }

    public int getTick() {
        return tick;
    }

    public String getMessage() {
        return message;
    }
}