package com.factoryonline.foundation.protocol;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public abstract class ProtocolDTO<T> {
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

    public final ProtocolDTOContainer toContainer() {
        return new ProtocolDTOContainer(id, version, serializeData());
    }

    public final String serialize() {
        return toContainer().serialize();
    }

    public static ProtocolDTOContainer deserialize(String serializedDto) {
        return ProtocolDTOContainer.deserialize(serializedDto);
    }

    public static DTOId resolveId(Class<? extends ProtocolDTO<?>> dtoClass) {
        Objects.requireNonNull(dtoClass, "dtoClass");

        try {
            Object idValue = dtoClass.getField("ID").get(null);
            if (!(idValue instanceof DTOId)) {
                throw new IllegalArgumentException("DTO class does not expose a DTOId field named ID: " + dtoClass.getName());
            }

            return (DTOId) idValue;
        } catch (IllegalAccessException | NoSuchFieldException exception) {
            throw new IllegalArgumentException("DTO class must expose a public static DTOId ID field: " + dtoClass.getName(), exception);
        }
    }

    public static <T, D extends ProtocolDTO<T>> T fromContainer(Class<D> dtoClass, ProtocolDTOContainer dtoContainer) {
        Objects.requireNonNull(dtoClass, "dtoClass");
        ProtocolDTOContainer validatedContainer = Objects.requireNonNull(dtoContainer, "dtoContainer");

        DTOId expectedId = resolveId(dtoClass);
        if (!expectedId.equals(validatedContainer.getId())) {
            throw new IllegalArgumentException(
                "DTO id mismatch. Expected " + expectedId + " but received " + validatedContainer.getId());
        }

        try {
            Method fromMethod = dtoClass.getMethod("from", String.class);
            Object decodedValue = fromMethod.invoke(null, validatedContainer.getData());
            return castDecodedValue(decodedValue);
        } catch (IllegalAccessException | NoSuchMethodException exception) {
            throw new IllegalArgumentException(
                "DTO class must expose a public static from(String) method: " + dtoClass.getName(),
                exception);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }

            throw new IllegalStateException("DTO decode failed for " + dtoClass.getName(), cause);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T castDecodedValue(Object decodedValue) {
        return (T) decodedValue;
    }

    protected abstract String serializeData();
}