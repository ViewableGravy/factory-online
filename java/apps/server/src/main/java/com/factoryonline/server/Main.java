package com.factoryonline.server;

import java.io.IOException;
import java.io.InputStreamReader;

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
        LocalTransportHub transportHub = new LocalTransportHub(INITIAL_SNAPSHOT_DELAY_TICKS);
        ClientApplication client = new ClientApplication(
            CLIENT_ID,
            SIMULATION_ID,
            transportHub.createClientTransport(CLIENT_ID)
        );
        ServerApplication server = new ServerApplication(transportHub.createServerTransport());

        server.setup();
        client.setup();
        
        server.handleJoinRequests();
        client.processIncomingMessages();

        try {
            try (CustomBufferedReader reader = new CustomBufferedReader(new InputStreamReader(System.in))) {
                System.out.print("Server input (Enter=tick, up/down=apply, exit=quit): ");
                CustomUserInput userInput;

                while ((userInput = reader.readLine()) != null) {
                    System.out.println("\nServer received: " + userInput.getRaw());

                    if (userInput.isExit())
                        break;

                    server.tick(userInput);
                    client.tick();
                    transportHub.advanceTick();

                    System.out.print("Server input (Enter=tick, up/down=apply, exit=quit): ");
                }
            }
        } finally {
            client.cleanup();
            server.cleanup();
        }
    }
}