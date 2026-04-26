package com.factoryonline.foundation.protocol;

import java.util.Objects;

public abstract class ProtocolDTO {
    public static final int CURRENT_VERSION = 1;

    private final DTOId id;
    private final int version;

    protected ProtocolDTO(DTOId id) {
        this(id, CURRENT_VERSION);
    }

    protected ProtocolDTO(DTOId id, int version) {
        this.id = Objects.requireNonNull(id, "id");
        if (version <= 0) {
            throw new IllegalArgumentException("version must be positive");
        }

        this.version = version;
    }

    public final DTOId getId() {
        return id;
    }

    public final int getVersion() {
        return version;
    }

    public final String serialize() {
        return new ProtocolDTOContainer(id, version, serializeData()).serialize();
    }

    public static ProtocolDTOContainer deserialize(String serializedDto) {
        return ProtocolDTOContainer.deserialize(serializedDto);
    }

    protected abstract String serializeData();
}