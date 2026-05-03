package com.factoryonline.transport.commands;

import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;

public final class ClientTransportCommand extends ProtocolCommand {
    public final ClientId clientId;
    public final ProtocolCommand payload;
    public final String sessionToken;

    public ClientTransportCommand(ClientId clientId, ProtocolCommand payload, String sessionToken) {
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.payload = Objects.requireNonNull(payload, "payload");
        this.sessionToken = sessionToken;
    }

    public ClientTransportCommand(ClientId clientId, ProtocolCommand payload) {
        this(clientId, payload, null);
    }
}
