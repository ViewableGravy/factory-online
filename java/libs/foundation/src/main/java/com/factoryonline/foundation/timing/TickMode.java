package com.factoryonline.foundation.timing;

import java.util.Locale;
import java.util.Objects;

public enum TickMode {
    AUTOMATIC("automatic"),
    MANUAL("manual");

    public final String protocolValue;

    TickMode(String protocolValue) {
        this.protocolValue = protocolValue;
    }

    public static TickMode fromValue(String value) {
        String normalizedValue = Objects.requireNonNull(value, "value").strip().toLowerCase(Locale.ROOT);
        for (TickMode mode : values()) {
            if (mode.protocolValue.equals(normalizedValue)) {
                return mode;
            }
        }

        throw new IllegalArgumentException("Unknown tick mode: " + value);
    }
}