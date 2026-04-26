package com.factoryonline.foundation.protocol;

import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;

public final class JoinSimulationRequestDTO extends ProtocolDTO<JoinSimulationRequest> {
    public static final String ID_VALUE = "join-simulation-request";
    public static final DTOId ID = new DTOId(ID_VALUE);

    private final SimulationId simulationId;

    public JoinSimulationRequestDTO(SimulationId simulationId) {
        super(ID);
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
    }

    @Override
    protected String serializeData() {
        return ProtocolJson.object(ProtocolJson.stringField("simulationId", simulationId.value()));
    }

    public static JoinSimulationRequest from(String data) {
        Map<String, String> fields = ProtocolJson.parseObject(data);
        return new JoinSimulationRequest(new SimulationId(ProtocolJson.requireString(fields, "simulationId")));
    }
}