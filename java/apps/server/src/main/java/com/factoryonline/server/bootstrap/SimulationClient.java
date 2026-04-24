package com.factoryonline.server.bootstrap;

import com.factoryonline.simulation.SimulationAugmentation;

public interface SimulationClient {
    void receiveBroadcast(String simulationName, SimulationAugmentation augmentation, int tick);
}