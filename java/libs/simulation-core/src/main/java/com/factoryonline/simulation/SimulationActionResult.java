package com.factoryonline.simulation;

public final class SimulationActionResult {
    public final boolean success;
    public final String error;

    private SimulationActionResult(boolean success, String error) {
        this.success = success;
        this.error = error;
    }

    public static SimulationActionResult success() {
        return new SimulationActionResult(true, null);
    }

    public static SimulationActionResult error(String error) {
        return new SimulationActionResult(false, error);
    }

}