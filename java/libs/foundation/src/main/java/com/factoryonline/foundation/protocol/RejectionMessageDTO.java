package com.factoryonline.foundation.protocol;

import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;

public final class RejectionMessageDTO extends ProtocolDTO<RejectionMessage> {
    public static final String ID_VALUE = "rejection-message";
    public static final DTOId ID = new DTOId(ID_VALUE);

    private final SimulationId simulationId;
    private final int tick;
    private final String message;

    public RejectionMessageDTO(SimulationId simulationId, int tick, String message) {
        super(ID);
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        this.tick = tick;
        this.message = Objects.requireNonNull(message, "message");
    }

    @Override
    protected String serializeData() {
        return ProtocolJson.object(
            ProtocolJson.stringField("simulationId", simulationId.value()),
            ProtocolJson.intField("tick", tick),
            ProtocolJson.stringField("message", message));
    }

    public static RejectionMessage from(String data) {
        Map<String, String> fields = ProtocolJson.parseObject(data);
        return new RejectionMessage(
            new SimulationId(ProtocolJson.requireString(fields, "simulationId")),
            ProtocolJson.requireInt(fields, "tick"),
            ProtocolJson.requireString(fields, "message"));
    }
}