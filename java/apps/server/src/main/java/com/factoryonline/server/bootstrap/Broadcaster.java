package com.factoryonline.server.bootstrap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.foundation.timing.TickControl;
import com.factoryonline.simulation.SimulationAugmentation;
import com.factoryonline.transport.ServerTransport;
import com.factoryonline.transport.commands.ProtocolCommand;
import com.factoryonline.transport.commands.SimulationUpdateCommand;
import com.factoryonline.transport.commands.TickSyncCommand;

public final class Broadcaster {
    private final ServerTransport transport;
    private final Map<SimulationId, List<ClientId>> subscribersBySimulation = new HashMap<>();

    public Broadcaster(ServerTransport transport) {
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

        sendToSubscribers(validatedSimulationId, new SimulationUpdateCommand(validatedSimulationId, augmentation, tick));
    }

    public void broadcastTickSync(SimulationId simulationId, int serverTick, int serverChecksum, TickControl tickControl) {
        SimulationId validatedSimulationId = Objects.requireNonNull(simulationId, "simulationId");
        TickControl validatedTickControl = Objects.requireNonNull(tickControl, "tickControl");

        sendToSubscribers(
            validatedSimulationId,
            new TickSyncCommand(validatedSimulationId, serverTick, serverChecksum, validatedTickControl));
    }

    private void sendToSubscribers(SimulationId simulationId, ProtocolCommand payload) {
        SimulationId validatedSimulationId = Objects.requireNonNull(simulationId, "simulationId");
        ProtocolCommand validatedPayload = Objects.requireNonNull(payload, "payload");

        List<ClientId> subscribers = subscribersBySimulation.get(validatedSimulationId);
        if (subscribers == null) {
            return;
        }

        for (ClientId clientId : List.copyOf(subscribers)) {
            try {
                transport.send(clientId, validatedPayload);
            } catch (IllegalArgumentException | IllegalStateException exception) {
                unsubscribe(validatedSimulationId, clientId);
                System.out.println(
                    "Server removed disconnected subscriber " + clientId.value
                        + " from " + validatedSimulationId.value);
            }
        }
    }

    private void unsubscribe(SimulationId simulationId, ClientId clientId) {
        SimulationId validatedSimulationId = Objects.requireNonNull(simulationId, "simulationId");
        ClientId validatedClientId = Objects.requireNonNull(clientId, "clientId");

        List<ClientId> subscribers = subscribersBySimulation.get(validatedSimulationId);
        if (subscribers == null) {
            return;
        }

        subscribers.remove(validatedClientId);
        if (subscribers.isEmpty()) {
            subscribersBySimulation.remove(validatedSimulationId);
        }
    }
}
