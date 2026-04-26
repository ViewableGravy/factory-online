package com.factoryonline.transport;

import java.util.List;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.protocol.ProtocolDTO;

public interface ClientTransport {
    ClientId getClientId();

    void send(TransportMessage message, boolean delayed);

    default void send(ProtocolDTO<?> dto, boolean delayed) {
        send(new TransportMessage(dto), delayed);
    }

    int getCurrentTick();

    <T, D extends ProtocolDTO<T>> List<T> drainAs(Class<D> dtoClass);
}