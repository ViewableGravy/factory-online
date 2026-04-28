package com.factoryonline.client;

import java.io.IOException;
import java.util.UUID;

import com.factoryonline.client.bootstrap.ClientApplication;
import com.factoryonline.client.bootstrap.ClientRuntimeLoop;
import com.factoryonline.foundation.config.NetworkConfig;
import com.factoryonline.foundation.config.TerminalCommands;
import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationIds;
import com.factoryonline.foundation.terminal.TerminalCommandHandler;
import com.factoryonline.server.bootstrap.TerminalUiState;
import com.factoryonline.transport.tcp.TcpClientTransport;

public final class Main {
    public static final ClientId clientId = new ClientId("client-" + UUID.randomUUID().toString().substring(0, 8));

    public static void main(String[] args) throws IOException {
        TcpClientTransport transport = new TcpClientTransport(
            NetworkConfig.DEFAULT_HOST,
            NetworkConfig.DEFAULT_PORT,
            clientId);
        ClientApplication client = new ClientApplication(clientId, SimulationIds.RANDOM, transport);
        ClientRuntimeLoop runtimeLoop = new ClientRuntimeLoop(client, transport);
        
        client.setup();
        runtimeLoop.start();

        printConnectedMessage();

        try {
            try (TerminalCommandHandler commandHandler = TerminalCommandHandler.createClientHandler()) {
                String rawCommand;

                while ((rawCommand = commandHandler.readCommand(prompt())) != null) {
                    String normalizedCommand = rawCommand.strip();

                    if (TerminalCommands.EXIT_COMMAND.equalsIgnoreCase(normalizedCommand)
                        || TerminalCommands.ESCAPE_COMMAND.equalsIgnoreCase(normalizedCommand)) {
                        break;
                    }

                    if (!normalizedCommand.isEmpty()) {
                        runtimeLoop.submitInput(rawCommand);
                    }
                }
            }
        } finally {
            runtimeLoop.stop();
            client.cleanup();
        }
    }

    private static String prompt() {
        return "Client ["
            + TerminalUiState.getInstance().formatClient(clientId)
            + "] "
            + TerminalCommands.SNAPSHOT_COMMAND
            + ", "
            + TerminalCommands.INCREMENT_COMMAND
            + "/"
            + TerminalCommands.DECREMENT_COMMAND
            + "=apply, "
            + TerminalCommands.EXIT_COMMAND
            + "=quit: ";
    }

    private static void printConnectedMessage() {
        System.out.println(
            "Client " + TerminalUiState.getInstance().formatClient(clientId) + " connected");
    }
}
