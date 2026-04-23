package com.factoryonline.server.bootstrap;

import java.util.concurrent.Phaser;

public class SimulationAction implements Runnable {
    private final Phaser phaser;
    private final SimulationBatch batch;
    private final Ticker ticker;

    public SimulationAction(Phaser phaser, SimulationBatch batch, Ticker ticker) {
        this.phaser = phaser;
        this.batch = batch;
        this.ticker = ticker;

        phaser.register();
    }

    @Override
    public void run() {
        int lastObservedTick = 0;

        while (true) {
            int currentTick;

            try {
                currentTick = ticker.awaitTick(lastObservedTick);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            if (ticker.isTerminated() || phaser.isTerminated())
                break;

            if (currentTick <= lastObservedTick)
                continue;

            runSimulationStep(currentTick);

            phaser.arrive();
            lastObservedTick = currentTick;
        }
    }

    private void runSimulationStep(int currentTick) {
        System.out.println(
            "Running batch of " + batch.size()
                + " simulations on tick " + currentTick
                + " in " + Thread.currentThread().getName());

        batch.run(currentTick);
    }
}
