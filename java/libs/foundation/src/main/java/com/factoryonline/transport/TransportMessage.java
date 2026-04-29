package com.factoryonline.transport;

import java.util.Objects;

import com.factoryonline.transport.commands.ProtocolCommand;

public final class TransportMessage {
    private final ProtocolCommand payload;

    public TransportMessage(ProtocolCommand command) {
        this.payload = Objects.requireNonNull(command, "command");
    }

    public ProtocolCommand getPayload() {
        return payload;
    }
}
