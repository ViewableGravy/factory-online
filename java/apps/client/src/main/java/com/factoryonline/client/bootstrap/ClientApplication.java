package com.factoryonline.client.bootstrap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class ClientApplication {
    private ClientApplication() {
    }

    public static void run(String[] args) throws IOException {
        System.out.println("Factory Online client scaffold");
        System.out.println("Networking is intentionally not implemented yet.");
        System.out.print("Enter text and press Enter: ");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String userInput;
        while ((userInput = reader.readLine()) != null) {
            System.out.println("Client received local input: " + userInput);
            System.out.print("Enter text and press Enter: ");
        }
    }
}