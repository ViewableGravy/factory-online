package com.factoryonline.server;

import java.io.IOException;

import com.factoryonline.client.bootstrap.ClientApplication;
import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.server.bootstrap.CustomBufferedReader;
import com.factoryonline.server.bootstrap.CustomUserInput;
import com.factoryonline.server.bootstrap.ServerApplication;
import com.factoryonline.server.bootstrap.TerminalUiState;
import com.factoryonline.transport.local.LocalTransportHub;

public final class Main {
    private static final int TRANSPORT_DELAY_TICKS = 2;
    private static final ClientId CLIENT_1_ID = new ClientId("client-1");
    private static final ClientId CLIENT_2_ID = new ClientId("client-2");
    private static final SimulationId CLIENT_1_SIMULATION_ID = new SimulationId("Simulation 1");
    private static final SimulationId CLIENT_2_SIMULATION_ID = new SimulationId("Simulation 2");
    private static final String SELECT_CLIENT_COMMAND = "/client";

    private Main() {
    }

    private static boolean isServerCommand(CustomUserInput userInput) {
        return userInput.getRaw().strip().startsWith("/");
    }

    public static void main(String[] args) throws IOException {
        LocalTransportHub transport = new LocalTransportHub(TRANSPORT_DELAY_TICKS);
        TerminalUiState terminalUiState = TerminalUiState.getInstance();
        terminalUiState.registerClient(CLIENT_1_ID, CLIENT_1_SIMULATION_ID, TerminalUiState.client1Color());
        terminalUiState.registerClient(CLIENT_2_ID, CLIENT_2_SIMULATION_ID, TerminalUiState.client2Color());

        ClientApplication client1 = new ClientApplication(
            CLIENT_1_ID,
            CLIENT_1_SIMULATION_ID,
            transport.createClientTransport(CLIENT_1_ID)
        );
        ClientApplication client2 = new ClientApplication(
            CLIENT_2_ID,
            CLIENT_2_SIMULATION_ID,
            transport.createClientTransport(CLIENT_2_ID)
        );
        ServerApplication server = new ServerApplication(transport.createServerTransport());

        server.setup();
        client1.setup();
        client2.setup();

        server.processIncomingMessages();
        client1.processIncomingMessages();
        client2.processIncomingMessages();

        try {
            try (CustomBufferedReader reader = new CustomBufferedReader(System.in)) {
                printPrompt();
                CustomUserInput userInput;

                while ((userInput = reader.readLine()) != null) {
                    System.out.println("\nInput received: " + userInput.getRaw());

                    if (userInput.isExit())
                        break;

                    if (handleClientSelectionCommand(userInput)) {
                        printPrompt();
                        continue;
                    }

                    if (isServerCommand(userInput)) {
                        server.handleAdminCommand(userInput.getRaw());
                        printPrompt();
                        continue;
                    }

                    server.advanceTick();
                    client1.advanceTick();
                    client2.advanceTick();

                    // This is tick aware so that we can mimic a network delay while ticks are manual
                    // In the future, this will be a network layer that will not hold onto the packet
                    // for a static amount of ticks, but instead be measured in MS
                    transport.advanceTick();

                    handleClientInput(client1, client2, userInput, terminalUiState);

                    server.processIncomingMessages();
                    client1.processIncomingMessages();
                    client2.processIncomingMessages();

                    server.simulateCurrentTick();
                    client1.simulateCurrentTick();
                    client2.simulateCurrentTick();

                    printPrompt();
                }
            }
        } finally {
            client1.cleanup();
            client2.cleanup();
            server.cleanup();
        }
    }

    private static boolean handleClientSelectionCommand(CustomUserInput userInput) {
        String normalizedInput = userInput.getRaw().strip();
        if (!normalizedInput.startsWith(SELECT_CLIENT_COMMAND + " ")) {
            return false;
        }

        String selection = normalizedInput.substring((SELECT_CLIENT_COMMAND + " ").length()).strip();
        TerminalUiState terminalUiState = TerminalUiState.getInstance();
        if (!terminalUiState.selectClient(selection)) {
            System.out.println("Unknown client selection: " + selection);
            return true;
        }

        System.out.println("Selected input client: " + terminalUiState.formatClient(terminalUiState.getSelectedClientId()));
        return true;
    }

    private static void handleClientInput(
        ClientApplication client1,
        ClientApplication client2,
        CustomUserInput userInput,
        TerminalUiState terminalUiState
    ) {
        if (userInput.getRaw().strip().isEmpty()) {
            return;
        }

        ClientId selectedClientId = terminalUiState.getSelectedClientId();
        if (CLIENT_1_ID.equals(selectedClientId)) {
            client1.handleInput(userInput);
            return;
        }

        if (CLIENT_2_ID.equals(selectedClientId)) {
            client2.handleInput(userInput);
        }
    }

    private static void printPrompt() {
        System.out.print(TerminalUiState.getInstance().prompt());
    }
}