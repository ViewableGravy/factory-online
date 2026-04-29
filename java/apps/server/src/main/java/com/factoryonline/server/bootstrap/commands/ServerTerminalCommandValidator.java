package com.factoryonline.server.bootstrap.commands;

import java.util.Objects;

import com.factoryonline.foundation.config.TerminalCommands;
import com.factoryonline.server.bootstrap.ServerApplication;
import com.factoryonline.server.bootstrap.ServerTickController;

public final class ServerTerminalCommandValidator {
    private ServerTerminalCommandValidator() {
    }

    public static Result validate(ServerTerminalCommand command, ServerApplication server, ServerTickController tickController) {
        ServerTerminalCommand validatedCommand = Objects.requireNonNull(command, "command");
        ServerApplication validatedServer = Objects.requireNonNull(server, "server");
        ServerTickController validatedTickController = Objects.requireNonNull(tickController, "tickController");

        if (validatedCommand instanceof ServerTerminalCommand.QueueManualTicks) {
            ServerTerminalCommand.QueueManualTicks queueManualTicks = (ServerTerminalCommand.QueueManualTicks) validatedCommand;
            if (queueManualTicks.count <= 0) {
                return Result.invalid("Tick count must be positive");
            }

            if (!validatedTickController.isManualTickMode()) {
                return Result.invalid(
                    "Server rejected tick request: switch to manual mode first with "
                        + TerminalCommands.TICK_MODE_USAGE);
            }
        }

        if (validatedCommand instanceof ServerTerminalCommand.UpdateTickRate) {
            ServerTerminalCommand.UpdateTickRate updateTickRate = (ServerTerminalCommand.UpdateTickRate) validatedCommand;
            if (updateTickRate.tickIntervalMillis <= 0) {
                return Result.invalid("Tick interval must be positive");
            }
        }

        if (validatedCommand instanceof ServerTerminalCommand.ApplyServerSimulationInput && !validatedServer.hasActiveSession()) {
            return Result.invalid(
                "Server ignored " + TerminalCommands.SERVER_COMMAND_PREFIX + " command: no active sessions");
        }

        return Result.valid();
    }

    public static final class Result {
        public final boolean valid;
        public final String message;

        private Result(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static Result valid() {
            return new Result(true, null);
        }

        public static Result invalid(String message) {
            return new Result(false, Objects.requireNonNull(message, "message"));
        }

    }
}
