package com.factoryonline.transport.local;

import java.util.List;
import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;

public final class LocalClientTransport {
    private final LocalTransportHub transportHub;
    private final ClientId clientId;

    LocalClientTransport(LocalTransportHub transportHub, ClientId clientId) {
        this.transportHub = Objects.requireNonNull(transportHub, "transportHub");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
    }

    public ClientId getClientId() {
        return clientId;
    }

    public void requestJoin(SimulationId simulationId) {
        transportHub.sendJoinRequest(new JoinSimulationRequest(clientId, simulationId));
    }

    public List<InitialSimulationState> drainInitialStates() {
        return transportHub.drainInitialStates(clientId);
    }

    public List<SimulationUpdate> drainSimulationUpdates() {
        return transportHub.drainSimulationUpdates(clientId);
    }
}