package com.factoryonline.server;

import java.io.IOException;

import com.factoryonline.client.bootstrap.ClientApplication;
import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.server.bootstrap.CustomBufferedReader;
import com.factoryonline.server.bootstrap.CustomUserInput;
import com.factoryonline.server.bootstrap.ServerApplication;
import com.factoryonline.transport.local.LocalTransportHub;

public final class Main {
    private static final int INITIAL_SNAPSHOT_DELAY_TICKS = 5;
    private static final ClientId CLIENT_ID = new ClientId("client-1");
    private static final SimulationId SIMULATION_ID = new SimulationId("Simulation 1");

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        LocalTransportHub transport = new LocalTransportHub(INITIAL_SNAPSHOT_DELAY_TICKS);
        ClientApplication client = new ClientApplication(
            CLIENT_ID,
            SIMULATION_ID,
            transport.createClientTransport(CLIENT_ID)
        );
        ServerApplication server = new ServerApplication(transport.createServerTransport());

        server.setup();
        client.setup();

        server.processIncomingMessages();
        client.processIncomingMessages();

        try {
            try (CustomBufferedReader reader = new CustomBufferedReader(System.in)) {
                System.out.print("Client input (Enter=tick, up/down=apply, exit=quit): ");
                CustomUserInput userInput;

                while ((userInput = reader.readLine()) != null) {
                    System.out.println("\nClient received: " + userInput.getRaw());

                    if (userInput.isExit())
                        break;

                    server.advanceTick();
                    client.advanceTick();

                    // This is tick aware so that we can mimic a network delay while ticks are manual
                    // In the future, this will be a network layer that will not hold onto the packet
                    // for a static amount of ticks, but instead be measured in MS
                    transport.advanceTick();

                    client.handleInput(userInput);

                    server.processIncomingMessages();
                    client.processIncomingMessages();

                    server.simulateCurrentTick();
                    client.simulateCurrentTick();

                    System.out.print("Client input (Enter=tick, up/down=apply, exit=quit): ");
                }
            }
        } finally {
            client.cleanup();
            server.cleanup();
        }
    }
}