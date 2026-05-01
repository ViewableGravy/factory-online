package com.factoryonline.client.bootstrap;

import java.util.UUID;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationIds;

public final class App {
    public static final ClientId clientId = new ClientId("client-" + UUID.randomUUID().toString().substring(0, 8));

    private static final ClientApplication INSTANCE = new ClientApplication(clientId, SimulationIds.RANDOM);

    private App() {
    }

    public static ClientApplication singleton() {
        return INSTANCE;
    }
}
