package com.factoryonline.foundation.protocol;

import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.simulation.SimulationAugmentation;

public final class SimulationInputRequestDTO extends ProtocolDTO {
    public static final String ID_VALUE = "simulation-input-request";
    public static final DTOId ID = new DTOId(ID_VALUE);

    private final ClientId clientId;
    private final SimulationId simulationId;
    private final SimulationAugmentation augmentation;

    public SimulationInputRequestDTO(ClientId clientId, SimulationId simulationId, SimulationAugmentation augmentation) {
        super(ID);
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.simulationId = Objects.requireNonNull(simulationId, "simulationId");
        this.augmentation = Objects.requireNonNull(augmentation, "augmentation");
    }

    @Override
    protected String serializeData() {
        return ProtocolJson.object(
            ProtocolJson.stringField("clientId", clientId.value()),
            ProtocolJson.stringField("simulationId", simulationId.value()),
            ProtocolJson.rawField("augmentation", serializeAugmentation(augmentation)));
    }

    public static SimulationInputRequest from(String data) {
        Map<String, String> fields = ProtocolJson.parseObject(data);
        return new SimulationInputRequest(
            new ClientId(ProtocolJson.requireString(fields, "clientId")),
            new SimulationId(ProtocolJson.requireString(fields, "simulationId")),
            deserializeAugmentation(ProtocolJson.requireRaw(fields, "augmentation")));
    }

    private static String serializeAugmentation(SimulationAugmentation augmentation) {
        return ProtocolJson.object(ProtocolJson.intField("valueDelta", augmentation.getValueDelta()));
    }

    private static SimulationAugmentation deserializeAugmentation(String data) {
        Map<String, String> fields = ProtocolJson.parseObject(data);
        return new SimulationAugmentation(ProtocolJson.requireInt(fields, "valueDelta"));
    }
}