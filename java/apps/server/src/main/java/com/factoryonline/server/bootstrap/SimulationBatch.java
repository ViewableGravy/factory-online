package com.factoryonline.server.bootstrap;

import java.util.ArrayList;
import java.util.List;

import com.factoryonline.simulation.Simulation;

final class SimulationBatch {
    private final List<Simulation> simulations = new ArrayList<>();

    synchronized void addSimulation(Simulation simulation) {
        simulations.add(simulation);
    }

    synchronized int size() {
        return simulations.size();
    }

    void run(int updateTick, boolean logThisTick) {
        List<Simulation> currentSimulations;
        synchronized (this) {
            currentSimulations = List.copyOf(simulations);
        }

        for (Simulation simulation : currentSimulations) {
            if (Thread.currentThread().isInterrupted())
                break;

            simulation.run(updateTick, logThisTick);
        }
    }
}