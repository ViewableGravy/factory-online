package com.factoryonline.transport.commands;

import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.foundation.timing.TickControl;

public final class TickSyncCommand extends ProtocolCommand {
    public final SimulationId simulationId;
    public final int serverTick;
    public final int serverChecksum;
    public final TickControl tickControl;

    public TickSyncCommand(SimulationId simulationId, int serverTick, int serverChecksum, TickControl tickControl) {
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        if (serverTick < 0) {
            throw new IllegalArgumentException("serverTick must not be negative");
        }

        this.serverTick = serverTick;
        this.serverChecksum = serverChecksum;
        this.tickControl = Objects.requireNonNull(tickControl, "tickControl");
    }
}
