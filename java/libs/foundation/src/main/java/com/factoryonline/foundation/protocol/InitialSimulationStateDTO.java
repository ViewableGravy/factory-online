package com.factoryonline.foundation.protocol;

import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.simulation.SimulationDirection;
import com.factoryonline.simulation.SimulationSnapshot;

public final class InitialSimulationStateDTO extends ProtocolDTO {
    public static final String ID_VALUE = "initial-simulation-state";
    public static final DTOId ID = new DTOId(ID_VALUE);

    private final SimulationId simulationId;
    private final SimulationSnapshot snapshot;
    private final int tick;

    public InitialSimulationStateDTO(SimulationId simulationId, SimulationSnapshot snapshot, int tick) {
        super(ID);
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        this.tick = tick;
    }

    @Override
    protected String serializeData() {
        return ProtocolJson.object(
            ProtocolJson.stringField("simulationId", simulationId.value()),
            ProtocolJson.rawField("snapshot", serializeSnapshot(snapshot)),
            ProtocolJson.intField("tick", tick));
    }

    public static InitialSimulationState from(String data) {
        Map<String, String> fields = ProtocolJson.parseObject(data);
        return new InitialSimulationState(
            new SimulationId(ProtocolJson.requireString(fields, "simulationId")),
            deserializeSnapshot(ProtocolJson.requireRaw(fields, "snapshot")),
            ProtocolJson.requireInt(fields, "tick"));
    }

    private static String serializeSnapshot(SimulationSnapshot snapshot) {
        return ProtocolJson.object(
            ProtocolJson.intField("value", snapshot.getValue()),
            ProtocolJson.stringField("direction", snapshot.getDirection().name()));
    }

    private static SimulationSnapshot deserializeSnapshot(String data) {
        Map<String, String> fields = ProtocolJson.parseObject(data);
        return new SimulationSnapshot(
            ProtocolJson.requireInt(fields, "value"),
            SimulationDirection.valueOf(ProtocolJson.requireString(fields, "direction")));
    }
}