package com.factoryonline.server;

import java.io.IOException;
import java.io.InputStreamReader;

import com.factoryonline.client.bootstrap.ClientApplication;
import com.factoryonline.server.bootstrap.CustomBufferedReader;
import com.factoryonline.server.bootstrap.CustomUserInput;
import com.factoryonline.server.bootstrap.ServerApplication;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws IOException {
        ClientApplication client = new ClientApplication();
        ServerApplication server = new ServerApplication(client);

        server.setup();
        client.setup();

        try {
            try (CustomBufferedReader reader = new CustomBufferedReader(new InputStreamReader(System.in))) {
                System.out.print("Enter text and press Enter: ");
                CustomUserInput userInput;

                while ((userInput = reader.readLine()) != null) {
                    System.out.println("\nServer received: " + userInput.getRaw());

                    if (userInput.isExit())
                        break;

                    server.run(userInput);
                    client.run();

                    System.out.print("Enter text and press Enter: ");
                }
            }
        } finally {
            client.cleanup();
            server.cleanup();
        }
    }
}