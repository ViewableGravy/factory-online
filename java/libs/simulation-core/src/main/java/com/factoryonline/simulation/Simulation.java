package com.factoryonline.simulation;

import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;

public final class Simulation {
    private static final String INDENT = "    ";
    private static final String ANSI_CYAN = "\u001B[36m";
    private static final String ANSI_RESET = "\u001B[0m";

    private final SimulationId id;
    private final SimulationState state;

    public Simulation(String name) {
        this(new SimulationId(name), new SimulationState());
    }

    public Simulation(String name, SimulationSnapshot snapshot) {
        this(new SimulationId(name), new SimulationState(snapshot));
    }

    public Simulation(SimulationId id) {
        this(id, new SimulationState());
    }

    public Simulation(SimulationId id, SimulationSnapshot snapshot) {
        this(id, new SimulationState(snapshot));
    }

    private Simulation(SimulationId id, SimulationState state) {
        this.id = Objects.requireNonNull(id, "id");
        this.state = Objects.requireNonNull(state, "state");
    }

    public SimulationId getId() {
        return id;
    }

    public String getName() {
        return id.value();
    }

    public void run(int updateTick) {
        state.advance();

        try {
            Thread.sleep(5L);
            System.out.println(
                INDENT + ANSI_CYAN + "[Thread]" + ANSI_RESET + " " + Thread.currentThread().getName()
                    + " [name: " + id + "] ran on [tick: " + updateTick + "] [value: " + state.getValue()
                    + "] [direction: " + state.getDirection() + "]");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public SimulationSnapshot snapshot() {
        return state.snapshot();
    }

    public SimulationActionResult applyAction(SimulationAugmentation augmentation) {
        Objects.requireNonNull(augmentation, "augmentation");

        boolean wasApplied = state.applyAugmentation(augmentation.getValueDelta());
        if (!wasApplied) {
            return SimulationActionResult.error(
                "augmentation would move simulation outside bounds for " + id);
        }

        return SimulationActionResult.success();
    }
}