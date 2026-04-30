package com.factoryonline.simulation.tick;

@FunctionalInterface
public interface TickHandler {
    void run(long tick);
}
