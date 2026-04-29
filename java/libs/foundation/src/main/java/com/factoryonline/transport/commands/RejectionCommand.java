package com.factoryonline.transport.commands;

import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;

public final class RejectionCommand extends ProtocolCommand {
    public final SimulationId simulationId;
    public final int tick;
    public final String message;

    public RejectionCommand(SimulationId simulationId, int tick, String message) {
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        this.tick = tick;
        this.message = Objects.requireNonNull(message, "message");
    }
}
