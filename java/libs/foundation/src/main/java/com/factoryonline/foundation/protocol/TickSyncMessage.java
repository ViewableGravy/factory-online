package com.factoryonline.foundation.protocol;

import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.foundation.timing.TickControl;

public final class TickSyncMessage {
    private final SimulationId simulationId;
    private final int serverTick;
    private final int serverChecksum;
    private final TickControl tickControl;

    public TickSyncMessage(SimulationId simulationId, int serverTick, int serverChecksum, TickControl tickControl) {
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        if (serverTick < 0) {
            throw new IllegalArgumentException("serverTick must not be negative");
        }

        this.serverTick = serverTick;
        this.serverChecksum = serverChecksum;
        this.tickControl = Objects.requireNonNull(tickControl, "tickControl");
    }

    public SimulationId getSimulationId() {
        return simulationId;
    }

    public int getServerTick() {
        return serverTick;
    }

    public int getServerChecksum() {
        return serverChecksum;
    }

    public TickControl getTickControl() {
        return tickControl;
    }
}
