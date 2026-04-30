package com.factoryonline.simulation.tick;

import java.util.ArrayDeque;
import java.util.Queue;

public final class Ticker {
    private final Object lock = new Object();
    private final Queue<Operation> queuedOperations = new ArrayDeque<>();
    private long currentTick;
    private boolean draining;

    public Ticker() {
        this(0L);
    }

    public Ticker(long initialTick) {
        if (initialTick < 0L) {
            throw new IllegalArgumentException("initialTick must not be negative");
        }

        this.currentTick = initialTick;
    }

    public void tick() {
        boolean shouldDrain = enqueue(new TickOperation());

        if (shouldDrain) {
            drainTicks();
        }
    }

    public void queueInitialize(long initialTick) {
        if (initialTick < 0L) {
            throw new IllegalArgumentException("initialTick must not be negative");
        }

        boolean shouldDrain = enqueue(new InitializeOperation(initialTick));

        if (shouldDrain) {
            drainTicks();
        }
    }

    public long getCurrentTick() {
        synchronized (lock) {
            return currentTick;
        }
    }

    private boolean enqueue(Operation operation) {
        synchronized (lock) {
            queuedOperations.add(operation);

            if (draining) {
                return false;
            }

            draining = true;
            return true;
        }
    }

    private void drainTicks() {
        while (true) {
            Operation operation;
            synchronized (lock) {
                operation = queuedOperations.poll();
                if (operation == null) {
                    draining = false;
                    return;
                }
            }

            try {
                operation.apply();
            } catch (RuntimeException | Error e) {
                stopDraining();
                throw e;
            }
        }
    }

    private void stopDraining() {
        synchronized (lock) {
            draining = false;
        }
    }

    private interface Operation {
        void apply();
    }

    private final class TickOperation implements Operation {
        @Override
        public void apply() {
            long tickToRun;
            synchronized (lock) {
                currentTick++;
                tickToRun = currentTick;
            }

            Scheduler.run(tickToRun);
        }
    }

    private final class InitializeOperation implements Operation {
        private final long initialTick;

        private InitializeOperation(long initialTick) {
            this.initialTick = initialTick;
        }

        @Override
        public void apply() {
            synchronized (lock) {
                currentTick = initialTick;
            }
        }
    }
}
