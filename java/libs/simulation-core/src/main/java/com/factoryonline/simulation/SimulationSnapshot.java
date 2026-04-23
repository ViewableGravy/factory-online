package com.factoryonline.simulation;

import java.util.Objects;

public final class SimulationSnapshot {
    private final int value;
    private final SimulationDirection direction;

    public SimulationSnapshot(int value, SimulationDirection direction) {
        validateValue(value);
        this.value = value;
        this.direction = Objects.requireNonNull(direction, "direction");
    }

    public int getValue() {
        return value;
    }

    public SimulationDirection getDirection() {
        return direction;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        SimulationSnapshot that = (SimulationSnapshot) other;
        return value == that.value && direction == that.direction;
    }

    @Override
    public int hashCode() {
        return 31 * Integer.hashCode(value) + direction.hashCode();
    }

    @Override
    public String toString() {
        return "SimulationSnapshot{" +
            "value=" + value +
            ", direction=" + direction +
            '}';
    }

    private static void validateValue(int value) {
        if (value < SimulationState.MIN_VALUE || value > SimulationState.MAX_VALUE) {
            throw new IllegalArgumentException(
                "value must be between " + SimulationState.MIN_VALUE + " and " + SimulationState.MAX_VALUE);
        }
    }
}