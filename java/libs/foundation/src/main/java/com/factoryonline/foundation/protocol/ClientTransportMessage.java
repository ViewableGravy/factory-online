package com.factoryonline.foundation.protocol;

import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;

public final class ClientTransportMessage {
    private final ClientId clientId;
    private final ProtocolDTOContainer payload;

    public ClientTransportMessage(ClientId clientId, ProtocolDTOContainer payload) {
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.payload = Objects.requireNonNull(payload, "payload");
    }

    public ClientId getClientId() {
        return clientId;
    }

    public ProtocolDTOContainer getPayload() {
        return payload;
    }
}