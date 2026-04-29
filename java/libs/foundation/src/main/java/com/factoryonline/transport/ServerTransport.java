package com.factoryonline.transport;

import java.util.List;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.transport.commands.ProtocolCommand;
import com.factoryonline.transport.commands.ClientTransportCommand;

public interface ServerTransport {
    List<ClientTransportCommand> drainMessages();

    void addMessageListener(Runnable listener);

    void send(ClientId clientId, TransportMessage message);

    default void send(ClientId clientId, ProtocolCommand command) {
        send(clientId, new TransportMessage(command));
    }
}
