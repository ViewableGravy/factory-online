package com.factoryonline.client.bootstrap.commands;

import java.util.Objects;

import com.factoryonline.simulation.SimulationAugmentation;

public abstract class ClientTerminalCommand {
    public static final class RequestSnapshot extends ClientTerminalCommand {
    }

    public static final class SendSimulationInput extends ClientTerminalCommand {
        public final SimulationAugmentation augmentation;

        public SendSimulationInput(SimulationAugmentation augmentation) {
            this.augmentation = Objects.requireNonNull(augmentation, "augmentation");
        }

    }
}
