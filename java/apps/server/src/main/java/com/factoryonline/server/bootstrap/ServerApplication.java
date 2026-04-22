package com.factoryonline.server.bootstrap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.factoryonline.simulation.NamedThreadFactory;

public final class ServerApplication {
    private ServerApplication() {
    }

    public static void run(String[] args) throws IOException {
        System.out.println("Factory Online server scaffold");
        System.out.println("Networking is intentionally not implemented yet.");
        System.out.print("Enter text and press Enter: ");

        NamedThreadFactory myThreadFactory = new NamedThreadFactory("SimulationThread");
        ExecutorService executorService = Executors.newFixedThreadPool(3, myThreadFactory);
        for (int i = 0; i < 5; i++) {
            executorService.execute(() -> System.out.println(Thread.currentThread().getName()));
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String userInput;
        while ((userInput = reader.readLine()) != null) {
            System.out.println("Server received: " + userInput);
            System.out.print("Enter text and press Enter: ");
        }
        executorService.shutdown();
    }
}