package com.factoryonline.transport.commands;

import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;

public final class JoinSimulationCommand extends ProtocolCommand {
    public final SimulationId simulationId;

    public JoinSimulationCommand(SimulationId simulationId) {
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
    }
}
