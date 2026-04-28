package com.factoryonline.server;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.factoryonline.foundation.config.NetworkConfig;
import com.factoryonline.foundation.config.RuntimeTiming;
import com.factoryonline.foundation.config.TerminalCommands;
import com.factoryonline.foundation.terminal.TerminalCommandHandler;
import com.factoryonline.foundation.timing.TickDeadline;
import com.factoryonline.server.bootstrap.CustomUserInput;
import com.factoryonline.server.bootstrap.ServerApplication;
import com.factoryonline.server.bootstrap.TerminalUiState;
import com.factoryonline.transport.tcp.TcpServerTransport;

public final class Main {
    private Main() {
    }

    private static boolean isAdminCommand(CustomUserInput userInput) {
        String normalizedInput = userInput.getRaw().strip();
        String serverCommandPrefix = TerminalCommands.SERVER_COMMAND_PREFIX + " ";
        return normalizedInput.startsWith("/") && !normalizedInput.startsWith(serverCommandPrefix);
    }

    public static void main(String[] args) throws IOException {
        Queue<String> queuedCommands = new ConcurrentLinkedQueue<>();
        AtomicBoolean running = new AtomicBoolean(true);

        try (TcpServerTransport transport = new TcpServerTransport(NetworkConfig.DEFAULT_PORT)) {
            ServerApplication server = new ServerApplication(transport);
            server.setup();

            System.out.println(
                TerminalUiState.getInstance().formatServerLabel() + " listening on port " + NetworkConfig.DEFAULT_PORT);

            Thread tickThread = startServerLoop(server, queuedCommands, running);
            try {
                try (TerminalCommandHandler commandHandler = TerminalCommandHandler.createServerHandler()) {
                    String rawCommand;

                    while ((rawCommand = commandHandler.readCommand(prompt())) != null) {
                        CustomUserInput userInput = CustomUserInput.fromRaw(rawCommand);

                        if (userInput.isExit()) {
                            running.set(false);
                            break;
                        }

                        if (!rawCommand.strip().isEmpty()) {
                            queuedCommands.add(rawCommand);
                        }
                    }
                }
            } finally {
                running.set(false);
                interruptAndJoin(tickThread);
                server.cleanup();
            }
        }
    }

    private static String extractServerCommand(CustomUserInput userInput) {
        String normalizedInput = userInput.getRaw().strip();
        String serverCommandPrefix = TerminalCommands.SERVER_COMMAND_PREFIX + " ";
        if (!normalizedInput.startsWith(serverCommandPrefix)) {
            return null;
        }

        String command = normalizedInput.substring(serverCommandPrefix.length()).strip();
        if (command.isEmpty()) {
            return null;
        }

        return command;
    }

    private static Thread startServerLoop(
        ServerApplication server,
        Queue<String> queuedCommands,
        AtomicBoolean running
    ) {
        Thread tickThread = new Thread(() -> {
            TickDeadline tickDeadline = new TickDeadline(RuntimeTiming.TICK_INTERVAL_NANOS);
            while (running.get()) {
                server.advanceTick();
                drainCommands(server, queuedCommands);
                server.processIncomingMessages();
                server.simulateCurrentTick();
                tickDeadline.sleepUntilNextTick();
            }
        }, "split-server-loop");
        tickThread.setDaemon(true);
        tickThread.start();
        return tickThread;
    }

    private static void drainCommands(ServerApplication server, Queue<String> queuedCommands) {
        String rawCommand;
        while ((rawCommand = queuedCommands.poll()) != null) {
            handleConsoleCommand(server, rawCommand);
        }
    }

    private static void handleConsoleCommand(ServerApplication server, String rawCommand) {
        CustomUserInput userInput = CustomUserInput.fromRaw(rawCommand);
        String serverCommand = extractServerCommand(userInput);
        if (serverCommand != null) {
            if (CustomUserInput.fromRaw(serverCommand).isContinue()) {
                System.out.println("Unknown " + TerminalCommands.SERVER_COMMAND_PREFIX + " command: " + serverCommand);
                return;
            }

            server.handleServerCommand(serverCommand);
            return;
        }

        if (isAdminCommand(userInput)) {
            server.handleAdminCommand(rawCommand);
            return;
        }

        System.out.println("Server ignored input: " + rawCommand.strip());
    }

    private static String prompt() {
        return "Server ["
            + TerminalCommands.SNAPSHOT_COMMAND
            + ", "
            + TerminalCommands.ADD_SIMULATION_COMMAND
            + ", "
            + TerminalCommands.SERVER_DIRECTION_USAGE
            + ", "
            + TerminalCommands.EXIT_COMMAND
            + "]: ";
    }

    private static void interruptAndJoin(Thread thread) {
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
