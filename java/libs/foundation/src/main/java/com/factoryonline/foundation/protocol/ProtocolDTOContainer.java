package com.factoryonline.foundation.protocol;

import java.util.Map;
import java.util.Objects;

public final class ProtocolDTOContainer {
    private final DTOId id;
    private final int version;
    private final String data;

    public ProtocolDTOContainer(DTOId id, int version, String data) {
        this.id = Objects.requireNonNull(id, "id");
        if (version <= 0) {
            throw new IllegalArgumentException("version must be positive");
        }

        this.version = version;
        this.data = Objects.requireNonNull(data, "data");
    }

    public DTOId getId() {
        return id;
    }

    public int getVersion() {
        return version;
    }

    public String getData() {
        return data;
    }

    public String serialize() {
        return ProtocolJson.object(
            ProtocolJson.stringField("id", id.value()),
            ProtocolJson.intField("version", version),
            ProtocolJson.rawField("data", data));
    }

    public static ProtocolDTOContainer deserialize(String serializedDto) {
        Map<String, String> fields = ProtocolJson.parseObject(serializedDto);
        DTOId id = new DTOId(ProtocolJson.requireString(fields, "id"));
        int version = ProtocolJson.requireInt(fields, "version");
        String data = ProtocolJson.requireRaw(fields, "data");
        return new ProtocolDTOContainer(id, version, data);
    }
}