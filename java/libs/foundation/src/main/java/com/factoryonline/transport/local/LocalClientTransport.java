package com.factoryonline.transport.local;

import java.util.List;
import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.foundation.protocol.JoinSimulationRequestDTO;
import com.factoryonline.foundation.protocol.ProtocolDTO;
import com.factoryonline.foundation.protocol.SimulationInputRequestDTO;
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
        transportHub.sendToServer(clientId, new JoinSimulationRequestDTO(simulationId), false);
    }

    public void sendSimulationInput(SimulationId simulationId, SimulationAugmentation augmentation) {
        transportHub.sendToServer(clientId, new SimulationInputRequestDTO(simulationId, augmentation), true);
    }

    public int getCurrentTick() {
        return transportHub.getCurrentTick();
    }

    public <T, D extends ProtocolDTO<T>> List<T> drainAs(Class<D> dtoClass) {
        return transportHub.drainClientAs(clientId, dtoClass);
    }
}