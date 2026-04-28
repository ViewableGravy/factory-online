package com.factoryonline.server;

import java.io.IOException;

import com.factoryonline.foundation.config.NetworkConfig;
import com.factoryonline.foundation.config.TerminalCommands;
import com.factoryonline.foundation.terminal.TerminalCommandHandler;
import com.factoryonline.server.bootstrap.ServerApplication;
import com.factoryonline.server.bootstrap.ServerRuntimeLoop;
import com.factoryonline.server.bootstrap.TerminalUiState;
import com.factoryonline.transport.tcp.TcpServerTransport;

public final class Main {
    private static ServerApplication server;
    private static ServerRuntimeLoop loop;

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        try (TcpServerTransport transport = new TcpServerTransport(NetworkConfig.DEFAULT_PORT)) {
            server = new ServerApplication(transport).configureDefault();
            loop = new ServerRuntimeLoop(server, transport);

            loop.start();

            System.out.println(
                TerminalUiState.getInstance().formatServerLabel() + " listening on port " + NetworkConfig.DEFAULT_PORT);

            TerminalCommandHandler commandHandler = TerminalCommandHandler.createServerHandler();
            String rawCommand;

            while ((rawCommand = commandHandler.readCommand(prompt())) != null) {
                String normalizedCommand = rawCommand.strip();

                if (TerminalCommands.EXIT_COMMAND.equalsIgnoreCase(normalizedCommand)) {
                    break;
                }

                if (!normalizedCommand.isEmpty()) {
                    loop.submitCommand(rawCommand);
                }
            }
        } finally {
            loop.stop();
            server.cleanup();
        }
    }

    private static String prompt() {
        return "Server ["
            + TerminalCommands.SNAPSHOT_COMMAND
            + ", "
            + TerminalCommands.ADD_SIMULATION_COMMAND
            + ", "
            + TerminalCommands.TICK_USAGE
            + ", "
            + TerminalCommands.TICK_MODE_USAGE
            + ", "
            + TerminalCommands.TICK_RATE_USAGE
            + ", "
            + TerminalCommands.SERVER_DIRECTION_USAGE
            + ", "
            + TerminalCommands.EXIT_COMMAND
            + "]: ";
    }
}
