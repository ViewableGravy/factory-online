package com.factoryonline.server.bootstrap;

public final class SimulationAction implements Runnable {
    private final SimulationBatch batch;
    private final int currentTick;
    private final String runtimeOwner;

    public SimulationAction(SimulationBatch batch, int currentTick, String runtimeOwner) {
        this.batch = batch;
        this.currentTick = currentTick;
        this.runtimeOwner = runtimeOwner;
    }

    @Override
    public void run() {
        System.out.println(
            "[" + runtimeOwner + "] Running batch of " + batch.size()
                + " simulations on tick " + currentTick
                + " in " + Thread.currentThread().getName());

        batch.run(currentTick);
    }
}
