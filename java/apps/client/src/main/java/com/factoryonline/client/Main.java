package com.factoryonline.client;

import java.io.IOException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.factoryonline.client.bootstrap.ClientApplication;
import com.factoryonline.foundation.config.NetworkConfig;
import com.factoryonline.foundation.config.RuntimeTiming;
import com.factoryonline.foundation.config.TerminalCommands;
import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationIds;
import com.factoryonline.foundation.terminal.TerminalCommandHandler;
import com.factoryonline.foundation.timing.TickDeadline;
import com.factoryonline.server.bootstrap.CustomUserInput;
import com.factoryonline.server.bootstrap.TerminalUiState;
import com.factoryonline.transport.tcp.TcpClientTransport;

public final class Main {
    public static final ClientId clientId = new ClientId("client-" + UUID.randomUUID().toString().substring(0, 8));
    public static final Queue<CustomUserInput> queuedInputs = new ConcurrentLinkedQueue<>();
    public static final AtomicBoolean running = new AtomicBoolean(true);

    public static void main(String[] args) throws IOException {
        TcpClientTransport transport = new TcpClientTransport(
            NetworkConfig.DEFAULT_HOST,
            NetworkConfig.DEFAULT_PORT,
            clientId);
        ClientApplication client = new ClientApplication(clientId, SimulationIds.RANDOM, transport);
        
        client.setup();

        printConnectedMessage();

        Thread tickThread = createTickThread(client, transport);

        try {
            try (TerminalCommandHandler commandHandler = TerminalCommandHandler.createClientHandler()) {
                String rawCommand;

                while ((rawCommand = commandHandler.readCommand(prompt())) != null) {
                    CustomUserInput userInput = CustomUserInput.fromRaw(rawCommand);

                    if (userInput.isExit()) {
                        running.set(false);
                        break;
                    }

                    if (!rawCommand.strip().isEmpty()) {
                        queuedInputs.add(userInput);
                    }
                }
            }
        } finally {
            running.set(false);
            
            tickThread.interrupt();
            try {
                tickThread.join();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }

            client.cleanup();
        }
    }

    private static Thread createTickThread(ClientApplication client, TcpClientTransport transport) {
        Thread tickThread = new Thread(() -> {
            TickDeadline tickDeadline = new TickDeadline(RuntimeTiming.TICK_INTERVAL_NANOS);
            while (running.get()) {
                client.advanceTick();
                transport.advanceTick();

                // Drain Inputs
                CustomUserInput userInput;
                while ((userInput = queuedInputs.poll()) != null) {
                    client.handleInput(userInput);
                }

                client.processIncomingMessages();
                client.simulateCurrentTick();

                tickDeadline.sleepUntilNextTick();
            }
        }, "split-client-loop");

        tickThread.setDaemon(true);
        tickThread.start();

        return tickThread;
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
