package com.factoryonline.transport.local;

import java.util.List;
import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.protocol.ProtocolDTO;
import com.factoryonline.transport.ClientTransport;
import com.factoryonline.transport.TransportMessage;

public final class LocalClientTransport implements ClientTransport {
    private final LocalTransportHub transportHub;
    private final ClientId clientId;

    LocalClientTransport(LocalTransportHub transportHub, ClientId clientId) {
        this.transportHub = Objects.requireNonNull(transportHub, "transportHub");
        this.clientId = Objects.requireNonNull(clientId, "clientId");
    }

    public ClientId getClientId() {
        return clientId;
    }

    public void send(TransportMessage message, boolean delayed) {
        transportHub.sendToServer(clientId, message, delayed);
    }

    @Override
    public void advanceTick() {
        transportHub.advanceTick();
    }

    @Override
    public void addMessageListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
    }

    public int getCurrentTick() {
        return transportHub.getCurrentTick();
    }

    public <T, D extends ProtocolDTO<T>> List<T> drainAs(Class<D> dtoClass) {
        return transportHub.drainClientAs(clientId, dtoClass);
    }
}