package com.factoryonline.foundation.protocol;

import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;

public final class ClientTransportMessageDTO extends ProtocolDTO<ClientTransportMessage> {
    public static final String ID_VALUE = "client-transport-message";
    public static final DTOId ID = new DTOId(ID_VALUE);

    private final ClientId clientId;
    private final ProtocolDTOContainer payload;

    public ClientTransportMessageDTO(ClientId clientId, ProtocolDTOContainer payload) {
        super(ID);
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.payload = Objects.requireNonNull(payload, "payload");
    }

    @Override
    protected String serializeData() {
        return ProtocolJson.object(
            ProtocolJson.stringField("clientId", clientId.value()),
            ProtocolJson.rawField("payload", payload.serialize()));
    }

    public static ClientTransportMessage from(String data) {
        Map<String, String> fields = ProtocolJson.parseObject(data);
        return new ClientTransportMessage(
            new ClientId(ProtocolJson.requireString(fields, "clientId")),
            ProtocolDTOContainer.deserialize(ProtocolJson.requireRaw(fields, "payload")));
    }
}