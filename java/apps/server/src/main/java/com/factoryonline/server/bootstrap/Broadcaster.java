package com.factoryonline.server.bootstrap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.factoryonline.simulation.SimulationAugmentation;

public final class Broadcaster {
    private static final Map<String, List<SimulationClient>> subscribersBySimulation = new HashMap<>();

    private Broadcaster() {
    }

    public static synchronized void subscribe(String simulationName, SimulationClient client) {
        subscribersBySimulation
            .computeIfAbsent(simulationName, ignored -> new ArrayList<>())
            .add(client);
    }

    public static synchronized void broadcast(String simulationName, SimulationAugmentation augmentation, int tick) {
        List<SimulationClient> subscribers = subscribersBySimulation.get(simulationName);
        if (subscribers == null) {
            return;
        }

        for (SimulationClient client : List.copyOf(subscribers)) {
            client.receiveBroadcast(simulationName, augmentation, tick);
        }
    }
}