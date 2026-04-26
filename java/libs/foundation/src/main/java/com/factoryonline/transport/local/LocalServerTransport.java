package com.factoryonline.transport.local;

import java.util.List;
import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.foundation.protocol.InitialSimulationStateDTO;
import com.factoryonline.foundation.protocol.ProtocolDTO;
import com.factoryonline.foundation.protocol.SimulationUpdateDTO;
import com.factoryonline.simulation.SimulationAugmentation;
import com.factoryonline.simulation.SimulationSnapshot;

public final class LocalServerTransport {
    private final LocalTransportHub transportHub;

    LocalServerTransport(LocalTransportHub transportHub) {
        this.transportHub = Objects.requireNonNull(transportHub, "transportHub");
    }

    public <T, D extends ProtocolDTO<T>> List<T> drainAs(Class<D> dtoClass) {
        return transportHub.drainServerAs(dtoClass);
    }

    public void sendInitialState(ClientId clientId, SimulationId simulationId, SimulationSnapshot snapshot, int tick) {
        transportHub.sendToClient(clientId, new InitialSimulationStateDTO(simulationId, snapshot, tick));
    }

    public void sendSimulationUpdate(ClientId clientId, SimulationId simulationId, SimulationAugmentation augmentation, int tick) {
        transportHub.sendToClient(clientId, new SimulationUpdateDTO(simulationId, augmentation, tick));
    }
}