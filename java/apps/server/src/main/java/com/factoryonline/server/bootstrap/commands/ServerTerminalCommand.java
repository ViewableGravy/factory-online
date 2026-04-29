package com.factoryonline.server.bootstrap.commands;

import java.util.Objects;

import com.factoryonline.foundation.timing.TickMode;
import com.factoryonline.simulation.SimulationAugmentation;

public abstract class ServerTerminalCommand {
    public static final class RequestSnapshot extends ServerTerminalCommand {
    }

    public static final class AddSimulation extends ServerTerminalCommand {
    }

    public static final class QueueManualTicks extends ServerTerminalCommand {
        public final int count;

        public QueueManualTicks(int count) {
            this.count = count;
        }

    }

    public static final class UpdateTickMode extends ServerTerminalCommand {
        public final TickMode tickMode;

        public UpdateTickMode(TickMode tickMode) {
            this.tickMode = Objects.requireNonNull(tickMode, "tickMode");
        }

    }

    public static final class UpdateTickRate extends ServerTerminalCommand {
        public final int tickIntervalMillis;

        public UpdateTickRate(int tickIntervalMillis) {
            this.tickIntervalMillis = tickIntervalMillis;
        }

    }

    public static final class ApplyServerSimulationInput extends ServerTerminalCommand {
        public final SimulationAugmentation augmentation;

        public ApplyServerSimulationInput(SimulationAugmentation augmentation) {
            this.augmentation = Objects.requireNonNull(augmentation, "augmentation");
        }

    }
}
