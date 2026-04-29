package com.factoryonline.client.bootstrap.commands;

import java.util.Objects;

import com.factoryonline.foundation.config.TerminalCommands;
import com.factoryonline.server.bootstrap.CustomUserInput;
import com.factoryonline.simulation.SimulationAugmentation;

public final class ClientTerminalCommandParser {
    private ClientTerminalCommandParser() {
    }

    public static Result parse(String rawCommand) {
        String normalizedCommand = Objects.requireNonNull(rawCommand, "rawCommand").strip();
        if (normalizedCommand.isEmpty()) {
            return Result.ignore();
        }

        if (TerminalCommands.SNAPSHOT_COMMAND.equalsIgnoreCase(normalizedCommand)) {
            return Result.command(new ClientTerminalCommand.RequestSnapshot());
        }

        SimulationAugmentation augmentation = toAugmentation(CustomUserInput.fromRaw(rawCommand));
        if (augmentation == null) {
            return Result.ignore();
        }

        return Result.command(new ClientTerminalCommand.SendSimulationInput(augmentation));
    }

    private static SimulationAugmentation toAugmentation(CustomUserInput userInput) {
        if (userInput.isIncrement()) {
            return new SimulationAugmentation(1);
        }

        if (userInput.isDecrement()) {
            return new SimulationAugmentation(-1);
        }

        return null;
    }

    public static final class Result {
        private final ClientTerminalCommand command;

        private Result(ClientTerminalCommand command) {
            this.command = command;
        }

        public static Result command(ClientTerminalCommand command) {
            return new Result(Objects.requireNonNull(command, "command"));
        }

        public static Result ignore() {
            return new Result(null);
        }

        public boolean hasCommand() {
            return command != null;
        }

        public ClientTerminalCommand getCommand() {
            return command;
        }
    }
}
