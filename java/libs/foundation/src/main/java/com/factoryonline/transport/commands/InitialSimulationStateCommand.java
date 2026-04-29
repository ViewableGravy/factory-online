package com.factoryonline.transport.commands;

import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.simulation.SimulationSnapshot;

public final class InitialSimulationStateCommand extends ProtocolCommand {
    public final SimulationId simulationId;
    public final SimulationSnapshot snapshot;
    public final int tick;

    public InitialSimulationStateCommand(SimulationId simulationId, SimulationSnapshot snapshot, int tick) {
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        if (tick < 0) {
            throw new IllegalArgumentException("tick must not be negative");
        }

        this.tick = tick;
    }
}
