package com.factoryonline.foundation.protocol;

import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;

public final class JoinSimulationRequestDTO extends ProtocolDTO<JoinSimulationRequest> {
    public static final String ID_VALUE = "join-simulation-request";
    public static final DTOId ID = new DTOId(ID_VALUE);

    private final ClientId clientId;
    private final SimulationId simulationId;

    public JoinSimulationRequestDTO(ClientId clientId, SimulationId simulationId) {
        super(ID);
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
    }

    @Override
    protected String serializeData() {
        return ProtocolJson.object(
            ProtocolJson.stringField("clientId", clientId.value()),
            ProtocolJson.stringField("simulationId", simulationId.value()));
    }

    public static JoinSimulationRequest from(String data) {
        Map<String, String> fields = ProtocolJson.parseObject(data);
        return new JoinSimulationRequest(
            new ClientId(ProtocolJson.requireString(fields, "clientId")),
            new SimulationId(ProtocolJson.requireString(fields, "simulationId")));
    }
}