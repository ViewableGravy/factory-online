package com.factoryonline.server.bootstrap;

import java.util.Objects;

import com.factoryonline.transport.ServerTransport;

public final class App {
    private static ServerApplication instance;

    private App() {
    }

    public static synchronized void initialize(ServerTransport transport) {
        if (instance != null) {
            throw new IllegalStateException("Server App already initialized");
        }

        instance = new ServerApplication(Objects.requireNonNull(transport, "transport")).configureDefault();
    }

    public static synchronized ServerApplication singleton() {
        if (instance == null) {
            throw new IllegalStateException("Server App not initialized");
        }

        return instance;
    }
}