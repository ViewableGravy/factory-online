package com.factoryonline.transport.local;

import java.util.List;
import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.foundation.protocol.AckMessageDTO;
import com.factoryonline.foundation.protocol.ClientTransportMessage;
import com.factoryonline.foundation.protocol.ClientTransportMessageDTO;
import com.factoryonline.foundation.protocol.InitialSimulationStateDTO;
import com.factoryonline.foundation.protocol.ProtocolDTO;
import com.factoryonline.foundation.protocol.RejectionMessageDTO;
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

    public List<ClientTransportMessage> drainClientMessages() {
        return transportHub.drainServerAs(ClientTransportMessageDTO.class);
    }

    public void sendInitialState(ClientId clientId, SimulationId simulationId, SimulationSnapshot snapshot, int tick) {
        transportHub.sendToClient(clientId, new InitialSimulationStateDTO(simulationId, snapshot, tick));
    }

    public void sendAck(ClientId clientId, SimulationId simulationId, int tick, String message) {
        transportHub.sendToClient(clientId, new AckMessageDTO(simulationId, tick, message));
    }

    public void sendRejection(ClientId clientId, SimulationId simulationId, int tick, String message) {
        transportHub.sendToClient(clientId, new RejectionMessageDTO(simulationId, tick, message));
    }

    public void sendSimulationUpdate(ClientId clientId, SimulationId simulationId, SimulationAugmentation augmentation, int tick) {
        transportHub.sendToClient(clientId, new SimulationUpdateDTO(simulationId, augmentation, tick));
    }
}