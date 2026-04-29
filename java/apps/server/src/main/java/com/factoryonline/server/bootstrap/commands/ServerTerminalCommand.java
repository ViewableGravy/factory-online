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
        private final int count;

        public QueueManualTicks(int count) {
            this.count = count;
        }

        public int getCount() {
            return count;
        }
    }

    public static final class UpdateTickMode extends ServerTerminalCommand {
        private final TickMode tickMode;

        public UpdateTickMode(TickMode tickMode) {
            this.tickMode = Objects.requireNonNull(tickMode, "tickMode");
        }

        public TickMode getTickMode() {
            return tickMode;
        }
    }

    public static final class UpdateTickRate extends ServerTerminalCommand {
        private final int tickIntervalMillis;

        public UpdateTickRate(int tickIntervalMillis) {
            this.tickIntervalMillis = tickIntervalMillis;
        }

        public int getTickIntervalMillis() {
            return tickIntervalMillis;
        }
    }

    public static final class ApplyServerSimulationInput extends ServerTerminalCommand {
        private final SimulationAugmentation augmentation;

        public ApplyServerSimulationInput(SimulationAugmentation augmentation) {
            this.augmentation = Objects.requireNonNull(augmentation, "augmentation");
        }

        public SimulationAugmentation getAugmentation() {
            return augmentation;
        }
    }
}
