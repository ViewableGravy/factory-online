package com.factoryonline.server.bootstrap;

public final class SimulationAction implements Runnable {
    private final SimulationBatch batch;
    private final int currentTick;
    private final boolean logThisTick;
    private final String runtimeOwner;

    public SimulationAction(SimulationBatch batch, int currentTick, String runtimeOwner, boolean logThisTick) {
        this.batch = batch;
        this.currentTick = currentTick;
        this.runtimeOwner = runtimeOwner;
        this.logThisTick = logThisTick;
    }

    @Override
    public void run() {
        if (logThisTick) {
            System.out.println(
                "[" + runtimeOwner + "] snapshot tick " + currentTick + " in " + Thread.currentThread().getName());
        }

        batch.run(currentTick, logThisTick);
    }
}
