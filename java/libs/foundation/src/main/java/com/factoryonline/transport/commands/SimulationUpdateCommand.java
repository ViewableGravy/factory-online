package com.factoryonline.transport.commands;

import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.simulation.SimulationAugmentation;

public final class SimulationUpdateCommand extends ProtocolCommand {
    public final SimulationId simulationId;
    public final SimulationAugmentation augmentation;
    public final int tick;

    public SimulationUpdateCommand(SimulationId simulationId, SimulationAugmentation augmentation, int tick) {
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        this.augmentation = Objects.requireNonNull(augmentation, "augmentation");
        if (tick <= 0) {
            throw new IllegalArgumentException("tick must be positive");
        }

        this.tick = tick;
    }
}
