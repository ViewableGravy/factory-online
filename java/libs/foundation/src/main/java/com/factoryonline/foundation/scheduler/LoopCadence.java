package com.factoryonline.foundation.scheduler;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.factoryonline.foundation.timing.TickControl;

/**
 * Utility class to manage the cadence of a runtime loop, supporting both fixed-interval and manual tick control.
 * 
 * A cadence is a rhythm or pattern of events. In the context of a runtime loop, it determines how often 
 * the loop should execute its cycle.
 */
public final class LoopCadence {
    private static int tickIntervalMillis = -1;
    private static long nextDeadlineNanos = -1L;
    private static boolean initialized;

    private LoopCadence() {
    }

    public static synchronized void initialize() {
        tickIntervalMillis = -1;
        nextDeadlineNanos = -1L;
        initialized = true;
    }

    public static synchronized boolean beginCycle(TickControl tickControl) {
        requireInitialized();
        
        TickControl validatedTickControl = Objects.requireNonNull(tickControl, "tickControl");

        if (validatedTickControl.isManual()) {
            nextDeadlineNanos = -1L;
            return false;
        }

        configureInterval(validatedTickControl);
        long nowNanos = System.nanoTime();
        if (nextDeadlineNanos < 0L) {
            nextDeadlineNanos = nowNanos + intervalNanos();
            return false;
        }

        if (nowNanos < nextDeadlineNanos) {
            return false;
        }

        nextDeadlineNanos += intervalNanos();
        return true;
    }

    public static synchronized long remainingNanos(TickControl tickControl) {
        requireInitialized();
        TickControl validatedTickControl = Objects.requireNonNull(tickControl, "tickControl");
        if (validatedTickControl.isManual()) {
            return Long.MAX_VALUE;
        }

        configureInterval(validatedTickControl);
        if (nextDeadlineNanos < 0L) {
            nextDeadlineNanos = System.nanoTime() + intervalNanos();
        }

        return nextDeadlineNanos - System.nanoTime();
    }

    private static void configureInterval(TickControl tickControl) {
        int nextTickIntervalMillis = tickControl.getTickIntervalMillis();
        if (tickIntervalMillis == nextTickIntervalMillis) {
            return;
        }

        tickIntervalMillis = nextTickIntervalMillis;
        nextDeadlineNanos = System.nanoTime() + intervalNanos();
    }

    private static long intervalNanos() {
        return TimeUnit.MILLISECONDS.toNanos(tickIntervalMillis);
    }

    private static void requireInitialized() {
        if (!initialized) {
            throw new IllegalStateException("LoopCadence must be initialized before use");
        }
    }
}
