package com.factoryonline.transport.commands;

import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;

public final class AckCommand extends ProtocolCommand {
    public final SimulationId simulationId;
    public final int tick;
    public final String message;

    public AckCommand(SimulationId simulationId, int tick, String message) {
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        this.tick = tick;
        this.message = Objects.requireNonNull(message, "message");
    }
}
