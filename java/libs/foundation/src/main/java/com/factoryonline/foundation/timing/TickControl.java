package com.factoryonline.foundation.timing;

import java.util.Objects;

public final class TickControl {
    private final TickMode mode;
    private final int tickIntervalMillis;

    private TickControl(TickMode mode, int tickIntervalMillis) {
        this.mode = Objects.requireNonNull(mode, "mode");
        if (tickIntervalMillis <= 0) {
            throw new IllegalArgumentException("tickIntervalMillis must be positive");
        }

        this.tickIntervalMillis = tickIntervalMillis;
    }

    public static TickControl automatic(int tickIntervalMillis) {
        return new TickControl(TickMode.AUTOMATIC, tickIntervalMillis);
    }

    public static TickControl manual(int tickIntervalMillis) {
        return new TickControl(TickMode.MANUAL, tickIntervalMillis);
    }

    public TickMode getMode() {
        return mode;
    }

    public int getTickIntervalMillis() {
        return tickIntervalMillis;
    }

    public boolean isAutomatic() {
        return mode == TickMode.AUTOMATIC;
    }

    public boolean isManual() {
        return mode == TickMode.MANUAL;
    }

    public TickControl withMode(TickMode nextMode) {
        return new TickControl(nextMode, tickIntervalMillis);
    }

    public TickControl withTickIntervalMillis(int nextTickIntervalMillis) {
        return new TickControl(mode, nextTickIntervalMillis);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof TickControl)) {
            return false;
        }

        TickControl that = (TickControl) other;
        return mode == that.mode && tickIntervalMillis == that.tickIntervalMillis;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, tickIntervalMillis);
    }
}