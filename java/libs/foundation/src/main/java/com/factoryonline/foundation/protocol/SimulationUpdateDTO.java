package com.factoryonline.foundation.protocol;

import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.simulation.SimulationAugmentation;

public final class SimulationUpdateDTO extends ProtocolDTO {
    public static final String ID_VALUE = "simulation-update";
    public static final DTOId ID = new DTOId(ID_VALUE);

    private final SimulationId simulationId;
    private final SimulationAugmentation augmentation;
    private final int tick;

    public SimulationUpdateDTO(SimulationId simulationId, SimulationAugmentation augmentation, int tick) {
        super(ID);
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        this.augmentation = Objects.requireNonNull(augmentation, "augmentation");
        this.tick = tick;
    }

    @Override
    protected String serializeData() {
        return ProtocolJson.object(
            ProtocolJson.stringField("simulationId", simulationId.value()),
            ProtocolJson.rawField("augmentation", serializeAugmentation(augmentation)),
            ProtocolJson.intField("tick", tick));
    }

    public static SimulationUpdate from(String data) {
        Map<String, String> fields = ProtocolJson.parseObject(data);
        return new SimulationUpdate(
            new SimulationId(ProtocolJson.requireString(fields, "simulationId")),
            deserializeAugmentation(ProtocolJson.requireRaw(fields, "augmentation")),
            ProtocolJson.requireInt(fields, "tick"));
    }

    private static String serializeAugmentation(SimulationAugmentation augmentation) {
        return ProtocolJson.object(ProtocolJson.intField("valueDelta", augmentation.getValueDelta()));
    }

    private static SimulationAugmentation deserializeAugmentation(String data) {
        Map<String, String> fields = ProtocolJson.parseObject(data);
        return new SimulationAugmentation(ProtocolJson.requireInt(fields, "valueDelta"));
    }
}