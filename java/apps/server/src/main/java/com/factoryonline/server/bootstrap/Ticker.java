package com.factoryonline.server.bootstrap;

import java.util.concurrent.atomic.AtomicInteger;

public final class Ticker {
    private final AtomicInteger tick = new AtomicInteger(0);
    private final Object tickMonitor = new Object();
    private volatile boolean terminated;

    public Ticker() {
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

    public int awaitTick(int previousTick) throws InterruptedException {
        synchronized (tickMonitor) {
            while (!terminated && tick.get() == previousTick) {
                tickMonitor.wait();
            }

            return tick.get();
        }
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