package com.factoryonline.simulation;

public final class SimulationActionResult {
    private final boolean success;
    private final String error;

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

    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }
}