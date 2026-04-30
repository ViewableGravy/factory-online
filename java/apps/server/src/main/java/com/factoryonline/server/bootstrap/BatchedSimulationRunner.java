package com.factoryonline.server.bootstrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.factoryonline.simulation.Simulation;

public final class BatchedSimulationRunner {
    private static final int DEFAULT_WORKER_COUNT = 4;
    private static final String DEFAULT_RUNTIME_OWNER = "runtime";

    private final List<SimulationBatch> batches;
    private final AtomicInteger nextBatchIndex = new AtomicInteger(0);
    private final AtomicBoolean snapshotRequested = new AtomicBoolean(false);
    private final String runtimeOwner;

    public BatchedSimulationRunner() {
        this(DEFAULT_WORKER_COUNT, DEFAULT_RUNTIME_OWNER);
    }

    public BatchedSimulationRunner(int workerCount) {
        this(workerCount, DEFAULT_RUNTIME_OWNER);
    }

    public BatchedSimulationRunner(int workerCount, String runtimeOwner) {
        this.runtimeOwner = validateNonBlank(runtimeOwner, "runtimeOwner");

        if (workerCount <= 0) {
            throw new IllegalArgumentException("workerCount must be positive");
        }

        this.batches = createBatches(workerCount);
    }

    public void addSimulation(Simulation simulation) {
        int batchIndex = nextBatchIndex.getAndIncrement() % batches.size();
        batches.get(batchIndex).addSimulation(simulation);
    }

    public void requestSnapshot() {
        snapshotRequested.set(true);
    }

    public void runTick(long tick) {
        if (tick <= 0L) {
            throw new IllegalArgumentException("tick must be positive");
        }

        boolean logThisTick = snapshotRequested.compareAndSet(true, false);
        for (SimulationBatch batch : batches) {
            if (logThisTick) {
                System.out.println(
                    "[" + runtimeOwner + "] snapshot tick " + tick + " in " + Thread.currentThread().getName());
            }

            batch.run(tick, logThisTick);
        }
    }

    public void close() {
    }

    private List<SimulationBatch> createBatches(int workerCount) {
        List<SimulationBatch> createdBatches = new ArrayList<>(workerCount);
        for (int batchIndex = 0; batchIndex < workerCount; batchIndex++) {
            createdBatches.add(new SimulationBatch());
        }

        return createdBatches;
    }

    private static String validateNonBlank(String value, String label) {
        String validatedValue = Objects.requireNonNull(value, label);
        if (validatedValue.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }

        return validatedValue;
    }
}
