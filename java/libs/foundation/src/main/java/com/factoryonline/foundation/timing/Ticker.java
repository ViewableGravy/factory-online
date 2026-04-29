package com.factoryonline.foundation.timing;

import java.util.concurrent.atomic.AtomicInteger;

public final class Ticker {
    private final AtomicInteger tick;
    private final Object tickMonitor = new Object();
    private volatile boolean terminated;

    public Ticker() {
        this(0);
    }

    public Ticker(int initialTick) {
        if (initialTick < 0) {
            throw new IllegalArgumentException("initialTick must not be negative");
        }

        this.tick = new AtomicInteger(initialTick);
    }

    public int tick() {
        int currentTick;

        synchronized (tickMonitor) {
            if (terminated) {
                return tick.get();
            }

            currentTick = tick.incrementAndGet();
            tickMonitor.notifyAll();
        }

        return currentTick;
    }

    public int getTick() {
        return tick.get();
    }

    public void shutdown() {
        terminated = true;

        synchronized (tickMonitor) {
            tickMonitor.notifyAll();
        }
    }

    public boolean isTerminated() {
        return terminated;
    }
}
