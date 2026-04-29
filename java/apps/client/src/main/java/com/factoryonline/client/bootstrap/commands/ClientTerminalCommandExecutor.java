package com.factoryonline.client.bootstrap.commands;

import java.util.Objects;

import com.factoryonline.client.bootstrap.ClientApplication;

public final class ClientTerminalCommandExecutor {
    private ClientTerminalCommandExecutor() {
    }

    public static void execute(ClientTerminalCommand command, ClientApplication client) {
        ClientTerminalCommand validatedCommand = Objects.requireNonNull(command, "command");
        ClientApplication validatedClient = Objects.requireNonNull(client, "client");

        if (validatedCommand instanceof ClientTerminalCommand.RequestSnapshot) {
            validatedClient.requestSnapshot();
            return;
        }

        if (validatedCommand instanceof ClientTerminalCommand.SendSimulationInput) {
            ClientTerminalCommand.SendSimulationInput sendSimulationInput =
                (ClientTerminalCommand.SendSimulationInput) validatedCommand;
            validatedClient.requestSimulationInput(sendSimulationInput.augmentation);
            return;
        }

        throw new IllegalArgumentException(
            "Unhandled client terminal command type: " + validatedCommand.getClass().getName());
    }
}
