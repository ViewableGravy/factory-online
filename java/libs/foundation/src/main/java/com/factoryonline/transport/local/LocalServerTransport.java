package com.factoryonline.transport.local;

import java.util.List;
import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.simulation.SimulationAugmentation;
import com.factoryonline.simulation.SimulationSnapshot;

public final class LocalServerTransport {
    private final LocalTransportHub transportHub;

    LocalServerTransport(LocalTransportHub transportHub) {
        this.transportHub = Objects.requireNonNull(transportHub, "transportHub");
    }

    public List<JoinSimulationRequest> drainJoinRequests() {
        return transportHub.drainJoinRequests();
    }

    public List<SimulationInputRequest> drainSimulationInputRequests() {
        return transportHub.drainSimulationInputRequests();
    }

    public void sendInitialState(ClientId clientId, SimulationId simulationId, SimulationSnapshot snapshot, int tick) {
        transportHub.sendInitialState(clientId, new InitialSimulationState(simulationId, snapshot, tick));
    }

    public void sendSimulationUpdate(ClientId clientId, SimulationId simulationId, SimulationAugmentation augmentation, int tick) {
        transportHub.sendSimulationUpdate(clientId, new SimulationUpdate(simulationId, augmentation, tick));
    }
}