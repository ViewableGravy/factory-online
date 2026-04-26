package com.factoryonline.foundation.protocol;

import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;

public final class TickSyncMessageDTO extends ProtocolDTO<TickSyncMessage> {
    public static final String ID_VALUE = "tick-sync-message";
    public static final DTOId ID = new DTOId(ID_VALUE);

    private final SimulationId simulationId;
    private final int serverTick;
    private final int serverChecksum;

    public TickSyncMessageDTO(SimulationId simulationId, int serverTick, int serverChecksum) {
        super(ID);
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        this.serverTick = serverTick;
        this.serverChecksum = serverChecksum;
    }

    @Override
    protected String serializeData() {
        return ProtocolJson.object(
            ProtocolJson.stringField("simulationId", simulationId.value()),
            ProtocolJson.intField("serverTick", serverTick),
            ProtocolJson.intField("serverChecksum", serverChecksum));
    }

    public static TickSyncMessage from(String data) {
        Map<String, String> fields = ProtocolJson.parseObject(data);
        return new TickSyncMessage(
            new SimulationId(ProtocolJson.requireString(fields, "simulationId")),
            ProtocolJson.requireInt(fields, "serverTick"),
            ProtocolJson.requireInt(fields, "serverChecksum"));
    }
}
