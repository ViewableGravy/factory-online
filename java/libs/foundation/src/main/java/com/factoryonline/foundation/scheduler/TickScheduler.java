package com.factoryonline.foundation.scheduler;

import java.util.Objects;

import com.factoryonline.foundation.timing.Ticker;

public final class TickScheduler {
    private final Ticker ticker;
    private final TickWork tickWork;
    private int queuedTicks;
    private int startupBufferTicks;
    private boolean runningTick;

    public TickScheduler(Ticker ticker, TickWork tickWork) {
        this.ticker = Objects.requireNonNull(ticker, "ticker");
        this.tickWork = Objects.requireNonNull(tickWork, "tickWork");
    }

    public synchronized void queueTicks(int tickCount) {
        if (tickCount < 0) {
            throw new IllegalArgumentException("tickCount must not be negative");
        }

        queuedTicks += tickCount;
    }

    public synchronized void setStartupBufferTicks(int startupBufferTicks) {
        if (startupBufferTicks < 0) {
            throw new IllegalArgumentException("startupBufferTicks must not be negative");
        }

        this.startupBufferTicks = startupBufferTicks;
    }

    public int getTick() {
        return ticker.getTick();
    }

    public int runQueuedTicks() {
        int completedTicks = 0;
        while (true) {
            int tick = beginNextTick();
            if (tick <= 0) {
                return completedTicks;
            }

            try {
                tickWork.runTick(tick);
                completedTicks += 1;
            } finally {
                finishTick();
            }
        }
    }

    public void shutdown() {
        ticker.shutdown();
    }

    private synchronized int beginNextTick() {
        if (runningTick || queuedTicks <= 0) {
            return -1;
        }

        queuedTicks -= 1;
        if (startupBufferTicks > 0) {
            startupBufferTicks -= 1;
            return -1;
        }

        runningTick = true;
        return ticker.tick();
    }

    private synchronized void finishTick() {
        runningTick = false;
    }
}
