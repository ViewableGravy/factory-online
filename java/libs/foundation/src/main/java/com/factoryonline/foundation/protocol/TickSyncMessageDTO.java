package com.factoryonline.foundation.protocol;

import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.foundation.timing.TickControl;
import com.factoryonline.foundation.timing.TickMode;

public final class TickSyncMessageDTO extends ProtocolDTO<TickSyncMessage> {
    public static final String ID_VALUE = "tick-sync-message";
    public static final DTOId ID = new DTOId(ID_VALUE);

    private final SimulationId simulationId;
    private final int serverTick;
    private final int serverChecksum;
    private final TickControl tickControl;

    public TickSyncMessageDTO(SimulationId simulationId, int serverTick, int serverChecksum, TickControl tickControl) {
        super(ID);
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        this.serverTick = serverTick;
        this.serverChecksum = serverChecksum;
        this.tickControl = Objects.requireNonNull(tickControl, "tickControl");
    }

    @Override
    protected String serializeData() {
        return ProtocolJson.object(
            ProtocolJson.stringField("simulationId", simulationId.value()),
            ProtocolJson.intField("serverTick", serverTick),
            ProtocolJson.intField("serverChecksum", serverChecksum),
            ProtocolJson.stringField("tickMode", tickControl.getMode().protocolValue()),
            ProtocolJson.intField("tickIntervalMillis", tickControl.getTickIntervalMillis()));
    }

    public static TickSyncMessage from(String data) {
        Map<String, String> fields = ProtocolJson.parseObject(data);
        return new TickSyncMessage(
            new SimulationId(ProtocolJson.requireString(fields, "simulationId")),
            ProtocolJson.requireInt(fields, "serverTick"),
            ProtocolJson.requireInt(fields, "serverChecksum"),
            ProtocolJson.requireString(fields, "tickMode").equals(TickMode.MANUAL.protocolValue())
                ? TickControl.manual(ProtocolJson.requireInt(fields, "tickIntervalMillis"))
                : TickControl.automatic(ProtocolJson.requireInt(fields, "tickIntervalMillis")));
    }
}
