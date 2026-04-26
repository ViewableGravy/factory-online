package com.factoryonline.server;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.factoryonline.server.bootstrap.CustomBufferedReader;
import com.factoryonline.server.bootstrap.CustomUserInput;
import com.factoryonline.server.bootstrap.ServerApplication;
import com.factoryonline.server.bootstrap.TerminalUiState;
import com.factoryonline.transport.tcp.TcpServerTransport;

public final class Main {
    private static final int DEFAULT_PORT = 9999;
    private static final int TICK_INTERVAL_MILLIS = 100;
    private static final String SERVER_COMMAND_PREFIX = "/server";

    private static final AtomicBoolean promptRequested = new AtomicBoolean(false);

    private Main() {
    }

    private static boolean isAdminCommand(CustomUserInput userInput) {
        return userInput.getRaw().strip().startsWith("/") && !userInput.getRaw().strip().startsWith(SERVER_COMMAND_PREFIX + " ");
    }

    public static void main(String[] args) throws IOException {
        Queue<String> queuedCommands = new ConcurrentLinkedQueue<>();
        AtomicBoolean running = new AtomicBoolean(true);

        try (TcpServerTransport transport = new TcpServerTransport(DEFAULT_PORT)) {
            ServerApplication server = new ServerApplication(transport);
            server.setup();

            System.out.println(TerminalUiState.getInstance().formatServerLabel() + " listening on port " + DEFAULT_PORT);

            Thread tickThread = startServerLoop(server, queuedCommands, running);
            try {
                try (CustomBufferedReader reader = new CustomBufferedReader(System.in)) {
                    printPrompt();
                    CustomUserInput userInput;

                    while ((userInput = reader.readLine()) != null) {
                        if (userInput.isExit()) {
                            running.set(false);
                            break;
                        }

                        if (!userInput.getRaw().strip().isEmpty()) {
                            queuedCommands.add(userInput.getRaw());
                        }

                        promptRequested.set(true);
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
        if (!normalizedInput.startsWith(SERVER_COMMAND_PREFIX + " ")) {
            return null;
        }

        String command = normalizedInput.substring((SERVER_COMMAND_PREFIX + " ").length()).strip();
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
            while (running.get()) {
                server.advanceTick();
                drainCommands(server, queuedCommands);
                server.processIncomingMessages();
                server.simulateCurrentTick();
                printPromptIfRequested(promptRequested, running);
                sleepTickInterval();
            }
        }, "split-server-loop");
        tickThread.setDaemon(true);
        tickThread.start();
        return tickThread;
    }

    private static void printPromptIfRequested(AtomicBoolean promptRequested, AtomicBoolean running) {
        if (promptRequested.getAndSet(false) && running.get()) {
            printPrompt();
        }
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
                System.out.println("Unknown /server command: " + serverCommand);
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

    private static void printPrompt() {
        System.out.print("Server [/snapshot, /add-simulation, /server up|down, exit]: ");
    }

    private static void interruptAndJoin(Thread thread) {
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static void sleepTickInterval() {
        try {
            Thread.sleep(TICK_INTERVAL_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}