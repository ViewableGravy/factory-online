package com.factoryonline.transport.local;

import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.simulation.SimulationAugmentation;

public final class SimulationInputRequest {
    private final ClientId clientId;
    private final SimulationId simulationId;
    private final SimulationAugmentation augmentation;

    public SimulationInputRequest(ClientId clientId, SimulationId simulationId, SimulationAugmentation augmentation) {
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        this.augmentation = Objects.requireNonNull(augmentation, "augmentation");
    }

    public ClientId getClientId() {
        return clientId;
    }

    public SimulationId getSimulationId() {
        return simulationId;
    }

    public SimulationAugmentation getAugmentation() {
        return augmentation;
    }
}