package com.factoryonline.transport.commands;

import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.simulation.SimulationAugmentation;

public final class SimulationInputCommand extends ProtocolCommand {
    public final SimulationId simulationId;
    public final SimulationAugmentation augmentation;

    public SimulationInputCommand(SimulationId simulationId, SimulationAugmentation augmentation) {
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        this.augmentation = Objects.requireNonNull(augmentation, "augmentation");
    }
}
