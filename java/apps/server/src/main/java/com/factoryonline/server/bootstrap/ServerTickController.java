package com.factoryonline.server.bootstrap;

import java.util.Objects;

import com.factoryonline.foundation.config.RuntimeTiming;
import com.factoryonline.foundation.timing.TickControl;
import com.factoryonline.foundation.timing.TickMode;

public final class ServerTickController {
    private TickControl tickControl;
    private int requestedManualTicks;

    public ServerTickController(TickControl tickControl) {
        this.tickControl = Objects.requireNonNull(tickControl, "tickControl");
    }

    public static ServerTickController automatic() {
        return new ServerTickController(TickControl.automatic(RuntimeTiming.TICK_INTERVAL_MILLIS));
    }

    public synchronized TickControl getTickControl() {
        return tickControl;
    }

    public synchronized boolean isManualTickMode() {
        return tickControl.isManual();
    }

    public synchronized int drainRequestedManualTicks() {
        int bufferedRequestedTicks = requestedManualTicks;
        requestedManualTicks = 0;
        return bufferedRequestedTicks;
    }

    public synchronized void queueManualTicks(int requestedTicks) {
        if (requestedTicks <= 0) {
            throw new IllegalArgumentException("requestedTicks must be positive");
        }

        if (!tickControl.isManual()) {
            throw new IllegalStateException("manual ticks require manual tick mode");
        }

        requestedManualTicks += requestedTicks;
    }

    public synchronized TickControl setTickMode(TickMode nextMode) {
        tickControl = tickControl.withMode(Objects.requireNonNull(nextMode, "nextMode"));
        if (nextMode == TickMode.AUTOMATIC) {
            requestedManualTicks = 0;
        }

        return tickControl;
    }

    public synchronized TickControl setTickIntervalMillis(int tickIntervalMillis) {
        if (tickIntervalMillis <= 0) {
            throw new IllegalArgumentException("tickIntervalMillis must be positive");
        }

        tickControl = tickControl.withTickIntervalMillis(tickIntervalMillis);
        return tickControl;
    }
}
