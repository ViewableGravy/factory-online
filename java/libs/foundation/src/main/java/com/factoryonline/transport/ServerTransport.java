package com.factoryonline.transport;

import java.util.List;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.protocol.ClientTransportMessage;
import com.factoryonline.foundation.protocol.ProtocolDTO;

public interface ServerTransport {
    List<ClientTransportMessage> drainMessages();

    void send(ClientId clientId, TransportMessage message);

    default void send(ClientId clientId, ProtocolDTO<?> dto) {
        send(clientId, new TransportMessage(dto));
    }
}