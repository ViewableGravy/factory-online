package com.factoryonline.server.bootstrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.factoryonline.simulation.NamedThreadFactory;
import com.factoryonline.simulation.Simulation;

public final class BatchedSimulationRunner {
    private static final int DEFAULT_WORKER_COUNT = 4;
    private static final String DEFAULT_RUNTIME_OWNER = "runtime";

    private final List<SimulationBatch> batches;
    private final ExecutorService executorService;
    private final AtomicInteger nextBatchIndex = new AtomicInteger(0);
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
        this.executorService =
            Executors.newFixedThreadPool(workerCount, new NamedThreadFactory(runtimeOwner + "-SimulationThread"));
    }

    public void addSimulation(Simulation simulation) {
        int batchIndex = nextBatchIndex.getAndIncrement() % batches.size();
        batches.get(batchIndex).addSimulation(simulation);
    }

    public void runTick(int tick) {
        if (tick <= 0) {
            throw new IllegalArgumentException("tick must be positive");
        }

        List<Future<?>> futures = new ArrayList<>(batches.size());
        for (SimulationBatch batch : batches) {
            futures.add(executorService.submit(new SimulationAction(batch, tick, runtimeOwner)));
        }

        for (var future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (ExecutionException e) {
                throw new IllegalStateException("Simulation batch failed for tick " + tick, e.getCause());
            }
        }
    }

    public void close() {
        executorService.shutdownNow();
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