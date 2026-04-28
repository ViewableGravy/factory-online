package com.factoryonline.server.bootstrap.commands;

import java.util.Objects;

import com.factoryonline.foundation.config.TerminalCommands;
import com.factoryonline.foundation.timing.TickMode;
import com.factoryonline.server.bootstrap.CustomUserInput;
import com.factoryonline.simulation.SimulationAugmentation;

public final class ServerTerminalCommandParser {
    public Result parse(String rawCommand) {
        String normalizedCommand = Objects.requireNonNull(rawCommand, "rawCommand").strip();
        if (normalizedCommand.isEmpty()) {
            return Result.ignore(null);
        }

        String serverCommandPrefix = TerminalCommands.SERVER_COMMAND_PREFIX + " ";
        if (normalizedCommand.startsWith(serverCommandPrefix)) {
            return parseServerSimulationCommand(normalizedCommand.substring(serverCommandPrefix.length()).strip());
        }

        if (!normalizedCommand.startsWith("/")) {
            return Result.ignore("Server ignored input: " + normalizedCommand);
        }

        if (TerminalCommands.SNAPSHOT_COMMAND.equalsIgnoreCase(normalizedCommand)) {
            return Result.command(new ServerTerminalCommand.RequestSnapshot());
        }

        if (TerminalCommands.ADD_SIMULATION_COMMAND.equalsIgnoreCase(normalizedCommand)) {
            return Result.command(new ServerTerminalCommand.AddSimulation());
        }

        if (startsWithCommand(normalizedCommand, TerminalCommands.TICK_MODE_COMMAND)) {
            return parseTickModeCommand(normalizedCommand);
        }

        if (startsWithCommand(normalizedCommand, TerminalCommands.TICK_RATE_COMMAND)) {
            return parseTickRateCommand(normalizedCommand);
        }

        if (startsWithCommand(normalizedCommand, TerminalCommands.TICK_COMMAND)) {
            return parseTickCommand(normalizedCommand);
        }

        return Result.invalid("Server ignored unknown command: " + normalizedCommand);
    }

    private static boolean startsWithCommand(String normalizedCommand, String command) {
        return normalizedCommand.equals(command) || normalizedCommand.startsWith(command + " ");
    }

    private Result parseServerSimulationCommand(String serverCommand) {
        if (serverCommand.isEmpty()) {
            return Result.invalid("Unknown " + TerminalCommands.SERVER_COMMAND_PREFIX + " command: " + serverCommand);
        }

        CustomUserInput userInput = CustomUserInput.fromRaw(serverCommand);
        SimulationAugmentation augmentation = toAugmentation(userInput);
        if (augmentation == null) {
            return Result.invalid("Unknown " + TerminalCommands.SERVER_COMMAND_PREFIX + " command: " + serverCommand);
        }

        return Result.command(new ServerTerminalCommand.ApplyServerSimulationInput(augmentation));
    }

    private Result parseTickCommand(String normalizedCommand) {
        String[] parts = normalizedCommand.split("\\s+");
        if (parts.length > 2) {
            return Result.invalid("Usage: " + TerminalCommands.TICK_USAGE);
        }

        int requestedTicks = 1;
        if (parts.length == 2) {
            try {
                requestedTicks = Integer.parseInt(parts[1]);
            } catch (NumberFormatException exception) {
                return Result.invalid("Usage: " + TerminalCommands.TICK_USAGE);
            }
        }

        return Result.command(new ServerTerminalCommand.QueueManualTicks(requestedTicks));
    }

    private Result parseTickModeCommand(String normalizedCommand) {
        String[] parts = normalizedCommand.split("\\s+");
        if (parts.length != 2) {
            return Result.invalid("Usage: " + TerminalCommands.TICK_MODE_USAGE);
        }

        try {
            return Result.command(new ServerTerminalCommand.UpdateTickMode(TickMode.fromValue(parts[1])));
        } catch (IllegalArgumentException exception) {
            return Result.invalid("Usage: " + TerminalCommands.TICK_MODE_USAGE);
        }
    }

    private Result parseTickRateCommand(String normalizedCommand) {
        String[] parts = normalizedCommand.split("\\s+");
        if (parts.length != 2) {
            return Result.invalid("Usage: " + TerminalCommands.TICK_RATE_USAGE);
        }

        try {
            return Result.command(new ServerTerminalCommand.UpdateTickRate(Integer.parseInt(parts[1])));
        } catch (NumberFormatException exception) {
            return Result.invalid("Usage: " + TerminalCommands.TICK_RATE_USAGE);
        }
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
        private final ServerTerminalCommand command;
        private final String message;

        private Result(ServerTerminalCommand command, String message) {
            this.command = command;
            this.message = message;
        }

        public static Result command(ServerTerminalCommand command) {
            return new Result(Objects.requireNonNull(command, "command"), null);
        }

        public static Result invalid(String message) {
            return new Result(null, Objects.requireNonNull(message, "message"));
        }

        public static Result ignore(String message) {
            return new Result(null, message);
        }

        public boolean hasCommand() {
            return command != null;
        }

        public ServerTerminalCommand getCommand() {
            return command;
        }

        public String getMessage() {
            return message;
        }
    }
}
