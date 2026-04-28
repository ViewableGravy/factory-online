package com.factoryonline.transport.local;

import java.util.List;
import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.protocol.ClientTransportMessage;
import com.factoryonline.foundation.protocol.ClientTransportMessageDTO;
import com.factoryonline.foundation.protocol.ProtocolDTO;
import com.factoryonline.transport.ServerTransport;
import com.factoryonline.transport.TransportMessage;

public final class LocalServerTransport implements ServerTransport {
    private final LocalTransportHub transportHub;

    LocalServerTransport(LocalTransportHub transportHub) {
        this.transportHub = Objects.requireNonNull(transportHub, "transportHub");
    }

    public <T, D extends ProtocolDTO<T>> List<T> drainAs(Class<D> dtoClass) {
        return transportHub.drainServerAs(dtoClass);
    }

    public List<ClientTransportMessage> drainMessages() {
        return transportHub.drainServerAs(ClientTransportMessageDTO.class);
    }

    @Override
    public void addMessageListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
    }

    public void send(ClientId clientId, TransportMessage message) {
        transportHub.sendToClient(clientId, message);
    }
}