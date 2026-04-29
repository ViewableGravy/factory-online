package com.factoryonline.client.bootstrap.commands;

import java.util.Objects;

import com.factoryonline.client.bootstrap.ClientApplication;

public final class ClientTerminalCommandValidator {
    private ClientTerminalCommandValidator() {
    }

    public static Result validate(ClientTerminalCommand command, ClientApplication client) {
        ClientTerminalCommand validatedCommand = Objects.requireNonNull(command, "command");
        ClientApplication validatedClient = Objects.requireNonNull(client, "client");

        if (validatedCommand instanceof ClientTerminalCommand.RequestSnapshot && !validatedClient.canRequestSnapshot()) {
            return Result.invalid(
                "Client " + validatedClient.getFormattedClientLabel() + " is still waiting for an initial snapshot");
        }

        if (validatedCommand instanceof ClientTerminalCommand.SendSimulationInput && !validatedClient.hasJoinedSimulation()) {
            return Result.invalid(
                "Client " + validatedClient.getFormattedClientLabel() + " is still waiting for an initial snapshot");
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
