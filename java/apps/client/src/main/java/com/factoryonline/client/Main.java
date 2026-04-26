package com.factoryonline.client;

import java.io.IOException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.factoryonline.client.bootstrap.ClientApplication;
import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationIds;
import com.factoryonline.server.bootstrap.CustomBufferedReader;
import com.factoryonline.server.bootstrap.CustomUserInput;
import com.factoryonline.server.bootstrap.TerminalUiState;
import com.factoryonline.transport.tcp.TcpClientTransport;

public final class Main {
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 9999;
    private static final int TICK_INTERVAL_MILLIS = 100;

    public static final ClientId clientId = new ClientId("client-" + UUID.randomUUID().toString().substring(0, 8));
    public static final Queue<CustomUserInput> queuedInputs = new ConcurrentLinkedQueue<>();
    public static final AtomicBoolean running = new AtomicBoolean(true);

    public static void main(String[] args) throws IOException {
        TcpClientTransport transport = new TcpClientTransport(DEFAULT_HOST, DEFAULT_PORT, clientId);
        ClientApplication client = new ClientApplication(clientId, SimulationIds.RANDOM, transport);

        client.setup();

        printConnectedMessage();

        Thread tickThread = createTickThread(client, transport);

        try{try (CustomBufferedReader reader = new CustomBufferedReader(System.in)) {
            printPrompt();
            CustomUserInput userInput;

            while ((userInput = reader.readLine()) != null) {
                if (userInput.isExit()) {
                    running.set(false);
                    break;
                }

                if (!userInput.getRaw().strip().isEmpty()) {
                    queuedInputs.add(userInput);
                }

                printPrompt();
            }
        }} finally {
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
                
                try {
                    Thread.sleep(TICK_INTERVAL_MILLIS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "split-client-loop");

        tickThread.setDaemon(true);
        tickThread.start();

        return tickThread;
    }

    private static void printPrompt() {
        System.out.print(
            "Client [" + TerminalUiState.getInstance().formatClient(clientId) + "] /snapshot, up/down=apply, exit=quit: ");
    }

    private static void printConnectedMessage() {
        System.out.println(
            "Client " + TerminalUiState.getInstance().formatClient(clientId) + " connected");
    }
}