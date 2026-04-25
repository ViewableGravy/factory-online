package com.factoryonline.foundation.ids;

import java.util.Objects;

public final class ClientId {
    private final String value;

    public ClientId(String value) {
        this.value = validateNonBlank(value, "value");
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        ClientId that = (ClientId) other;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }

    private static String validateNonBlank(String value, String label) {
        String validatedValue = Objects.requireNonNull(value, label);
        if (validatedValue.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }

        return validatedValue;
    }
}