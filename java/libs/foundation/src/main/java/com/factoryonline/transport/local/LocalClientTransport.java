package com.factoryonline.transport.local;

import java.util.List;
import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.foundation.protocol.InitialSimulationState;
import com.factoryonline.foundation.protocol.JoinSimulationRequestDTO;
import com.factoryonline.foundation.protocol.SimulationInputRequestDTO;
import com.factoryonline.foundation.protocol.SimulationUpdate;
import com.factoryonline.simulation.SimulationAugmentation;

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
        transportHub.sendJoinRequest(new JoinSimulationRequestDTO(clientId, simulationId));
    }

    public void sendSimulationInput(SimulationId simulationId, SimulationAugmentation augmentation) {
        transportHub.sendSimulationInputRequest(new SimulationInputRequestDTO(clientId, simulationId, augmentation));
    }

    public int getCurrentTick() {
        return transportHub.getCurrentTick();
    }

    public List<InitialSimulationState> drainInitialStates() {
        return transportHub.drainInitialStates(clientId);
    }

    public List<SimulationUpdate> drainSimulationUpdates() {
        return transportHub.drainSimulationUpdates(clientId);
    }
}