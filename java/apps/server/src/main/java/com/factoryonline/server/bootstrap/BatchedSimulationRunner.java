package com.factoryonline.server.bootstrap;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

import com.factoryonline.simulation.NamedThreadFactory;
import com.factoryonline.simulation.Simulation;

public final class BatchedSimulationRunner {
    private static final int DEFAULT_WORKER_COUNT = 4;
    private static final String DEFAULT_RUNTIME_OWNER = "runtime";

    private final Ticker ticker;
    private final List<SimulationBatch> batches;
    private final Phaser phaser;
    private final ExecutorService executorService;
    private final AtomicInteger nextBatchIndex = new AtomicInteger(0);
    private final int startingTick;
    private final String runtimeOwner;

    public BatchedSimulationRunner(Ticker ticker) {
        this(ticker, DEFAULT_WORKER_COUNT, DEFAULT_RUNTIME_OWNER);
    }

    public BatchedSimulationRunner(Ticker ticker, int workerCount) {
        this(ticker, workerCount, DEFAULT_RUNTIME_OWNER);
    }

    public BatchedSimulationRunner(Ticker ticker, int workerCount, String runtimeOwner) {
        this.ticker = Objects.requireNonNull(ticker, "ticker");
        this.startingTick = ticker.getTick();
        this.runtimeOwner = validateNonBlank(runtimeOwner, "runtimeOwner");

        if (workerCount <= 0) {
            throw new IllegalArgumentException("workerCount must be positive");
        }

        this.phaser = new Phaser(0);
        this.batches = createBatches(workerCount);
        this.executorService =
            Executors.newFixedThreadPool(workerCount, new NamedThreadFactory(runtimeOwner + "-SimulationThread"));

        startWorkers();
    }

    public void addSimulation(Simulation simulation) {
        int batchIndex = nextBatchIndex.getAndIncrement() % batches.size();
        batches.get(batchIndex).addSimulation(simulation);
    }

    public void awaitCompletion(int tick) {
        if (tick <= startingTick) {
            return;
        }

        phaser.awaitAdvance((tick - startingTick) - 1);
    }

    public void close() {
        ticker.shutdown();
        phaser.forceTermination();
        executorService.shutdownNow();
    }

    private void startWorkers() {
        for (SimulationBatch batch : batches) {
            executorService.submit(new SimulationAction(phaser, batch, ticker, startingTick, runtimeOwner));
        }
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