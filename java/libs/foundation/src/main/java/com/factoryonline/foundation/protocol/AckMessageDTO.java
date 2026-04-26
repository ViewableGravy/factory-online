package com.factoryonline.foundation.protocol;

import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;

public final class AckMessageDTO extends ProtocolDTO<AckMessage> {
    public static final String ID_VALUE = "ack-message";
    public static final DTOId ID = new DTOId(ID_VALUE);

    private final SimulationId simulationId;
    private final int tick;
    private final String message;

    public AckMessageDTO(SimulationId simulationId, int tick, String message) {
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

    public static AckMessage from(String data) {
        Map<String, String> fields = ProtocolJson.parseObject(data);
        return new AckMessage(
            new SimulationId(ProtocolJson.requireString(fields, "simulationId")),
            ProtocolJson.requireInt(fields, "tick"),
            ProtocolJson.requireString(fields, "message"));
    }
}