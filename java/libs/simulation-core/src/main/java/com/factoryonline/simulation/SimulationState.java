package com.factoryonline.simulation;

import java.util.Objects;

public final class SimulationState {
    static final int MIN_VALUE = 0;
    static final int MAX_VALUE = 100;

    private int value;
    private SimulationDirection direction;

    public SimulationState() {
        this(MIN_VALUE, SimulationDirection.INCREASING);
    }

    public SimulationState(SimulationSnapshot snapshot) {
        this(Objects.requireNonNull(snapshot, "snapshot").getValue(), snapshot.getDirection());
    }

    public SimulationState(int value, SimulationDirection direction) {
        validateValue(value);
        this.value = value;
        this.direction = Objects.requireNonNull(direction, "direction");
    }

    public synchronized void advance() {
        if (direction == SimulationDirection.INCREASING) {
            if (value < MAX_VALUE) {
                value++;
            }

            if (value == MAX_VALUE) {
                direction = SimulationDirection.DECREASING;
            }

            return;
        }

        if (value > MIN_VALUE) {
            value--;
        }

        if (value == MIN_VALUE) {
            direction = SimulationDirection.INCREASING;
        }
    }

    public synchronized int getValue() {
        return value;
    }

    public synchronized boolean applyAugmentation(int valueDelta) {
        int nextValue = value + valueDelta;
        if (nextValue < MIN_VALUE || nextValue > MAX_VALUE) {
            return false;
        }

        value = nextValue;
        return true;
    }

    public synchronized SimulationDirection getDirection() {
        return direction;
    }

    public synchronized SimulationSnapshot snapshot() {
        return new SimulationSnapshot(value, direction);
    }

    private static void validateValue(int value) {
        if (value < MIN_VALUE || value > MAX_VALUE) {
            throw new IllegalArgumentException("value must be between " + MIN_VALUE + " and " + MAX_VALUE);
        }
    }
}