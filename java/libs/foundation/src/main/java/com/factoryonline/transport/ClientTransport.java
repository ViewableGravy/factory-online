package com.factoryonline.transport;

import java.util.List;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.transport.commands.ProtocolCommand;

public interface ClientTransport {
    ClientId getClientId();

    void send(TransportMessage message, boolean delayed);

    void advanceTick();

    void addMessageListener(Runnable listener);

    default void send(ProtocolCommand command, boolean delayed) {
        send(new TransportMessage(command), delayed);
    }

    int getCurrentTick();

    <T extends ProtocolCommand> List<T> drainAs(Class<T> commandClass);
}
