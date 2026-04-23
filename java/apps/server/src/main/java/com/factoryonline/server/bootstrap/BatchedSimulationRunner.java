package com.factoryonline.server.bootstrap;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

import com.factoryonline.simulation.NamedThreadFactory;
import com.factoryonline.simulation.Simulation;

public final class BatchedSimulationRunner {
    private static final int MAX_WORKERS = 4;

    private final Ticker ticker;
    private final List<SimulationBatch> batches;
    private final Phaser phaser;
    private final ExecutorService executorService;
    private final AtomicInteger nextBatchIndex = new AtomicInteger(0);

    public BatchedSimulationRunner(Ticker ticker) {
        this.ticker = ticker;
        this.phaser = new Phaser(0);
        this.batches = createBatches();
        this.executorService =
            Executors.newFixedThreadPool(batches.size(), new NamedThreadFactory("SimulationThread"));

        startWorkers();
    }

    public void addSimulation(Simulation simulation) {
        int batchIndex = nextBatchIndex.getAndIncrement() % batches.size();
        batches.get(batchIndex).addSimulation(simulation);
    }

    public void awaitCompletion(int tick) {
        if (tick <= 0)
            return;

        phaser.awaitAdvance(tick - 1);
    }

    public void close() {
        ticker.shutdown();
        phaser.forceTermination();
        executorService.shutdownNow();
    }

    private void startWorkers() {
        for (SimulationBatch batch : batches) {
            executorService.submit(new SimulationAction(phaser, batch, ticker));
        }
    }

    private List<SimulationBatch> createBatches() {
        List<SimulationBatch> createdBatches = new ArrayList<>(MAX_WORKERS);
        for (int batchIndex = 0; batchIndex < MAX_WORKERS; batchIndex++) {
            createdBatches.add(new SimulationBatch());
        }

        return createdBatches;
    }
}