package com.factoryonline.foundation.timing;

import java.util.concurrent.locks.LockSupport;

public final class TickDeadline {
    private final long tickIntervalNanos;
    private long nextDeadlineNanos;

    public TickDeadline(long tickIntervalNanos) {
        if (tickIntervalNanos <= 0L) {
            throw new IllegalArgumentException("tickIntervalNanos must be positive");
        }

        this.tickIntervalNanos = tickIntervalNanos;
        this.nextDeadlineNanos = System.nanoTime() + tickIntervalNanos;
    }

    public void sleepUntilNextTick() {
        long nowNanos = System.nanoTime();
        long remainingNanos = nextDeadlineNanos - nowNanos;
        if (remainingNanos > 0L) {
            LockSupport.parkNanos(remainingNanos);
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
        }

        nowNanos = System.nanoTime();
        nextDeadlineNanos += tickIntervalNanos;
        if (nextDeadlineNanos <= nowNanos) {
            nextDeadlineNanos = nowNanos + tickIntervalNanos;
        }
    }
}
