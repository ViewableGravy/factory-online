package com.factoryonline.transport;

import java.util.Objects;

import com.factoryonline.foundation.protocol.ProtocolDTO;
import com.factoryonline.foundation.protocol.ProtocolDTOContainer;

public final class TransportMessage {
    private final ProtocolDTOContainer payload;

    public TransportMessage(ProtocolDTO<?> dto) {
        this(Objects.requireNonNull(dto, "dto").toContainer());
    }

    public TransportMessage(ProtocolDTOContainer payload) {
        this.payload = Objects.requireNonNull(payload, "payload");
    }

    public ProtocolDTOContainer getPayload() {
        return payload;
    }
}