package com.factoryonline.simulation;

import java.util.Objects;

public final class Simulation {
    private final String name;
    private final SimulationState state;

    public Simulation(String name) {
        this(name, new SimulationState());
    }

    public Simulation(String name, SimulationSnapshot snapshot) {
        this(name, new SimulationState(snapshot));
    }

    private Simulation(String name, SimulationState state) {
        this.name = validateName(name);
        this.state = Objects.requireNonNull(state, "state");
    }

    public String getName() {
        return name;
    }

    public void run(int updateTick) {
        state.advance();

        try {
            Thread.sleep(5L);
            System.out.println(
                "[name: " + name + "] ran on [tick: " + updateTick + "] [value: " + state.getValue()
                    + "] [direction: " + state.getDirection() + "] in [thread: "
                    + Thread.currentThread().getName() + "]");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public SimulationSnapshot snapshot() {
        return state.snapshot();
    }

    private static String validateName(String name) {
        String validatedName = Objects.requireNonNull(name, "name");

        if (validatedName.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }

        return validatedName;
    }
}