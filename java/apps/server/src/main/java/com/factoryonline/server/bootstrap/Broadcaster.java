package com.factoryonline.server.bootstrap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.simulation.SimulationAugmentation;
import com.factoryonline.transport.local.LocalServerTransport;

public final class Broadcaster {
    private final LocalServerTransport transport;
    private final Map<SimulationId, List<ClientId>> subscribersBySimulation = new HashMap<>();

    public Broadcaster(LocalServerTransport transport) {
        this.transport = Objects.requireNonNull(transport, "transport");
    }

    public void subscribe(SimulationId simulationId, ClientId clientId) {
        SimulationId validatedSimulationId = Objects.requireNonNull(simulationId, "simulationId");
        ClientId validatedClientId = Objects.requireNonNull(clientId, "clientId");

        List<ClientId> subscribers = subscribersBySimulation
            .computeIfAbsent(validatedSimulationId, ignored -> new ArrayList<>());
        if (subscribers.contains(validatedClientId))
            return;

        subscribers.add(validatedClientId);
    }

    public void broadcast(SimulationId simulationId, int tick, SimulationAugmentation augmentation) {
        SimulationId validatedSimulationId = Objects.requireNonNull(simulationId, "simulationId");
        Objects.requireNonNull(augmentation, "augmentation");

        List<ClientId> subscribers = subscribersBySimulation.get(validatedSimulationId);
        if (subscribers == null) {
            return;
        }

        for (ClientId clientId : List.copyOf(subscribers)) {
            transport.sendSimulationUpdate(clientId, validatedSimulationId, augmentation, tick);
        }
    }
}