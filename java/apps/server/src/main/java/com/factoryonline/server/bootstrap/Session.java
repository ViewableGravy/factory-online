package com.factoryonline.server.bootstrap;

import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;

public final class Session {
    private final ClientId clientId;
    private final SimulationId simulationId;

    public Session(ClientId clientId, SimulationId simulationId) {
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
    }

    public ClientId getClientId() {
        return clientId;
    }

    public SimulationId getSimulationId() {
        return simulationId;
    }
}