package com.factoryonline.server.bootstrap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.config.TerminalCommands;
import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;

public final class TerminalUiState {
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_CLIENT_1 = "\u001B[36m";
    private static final String ANSI_CLIENT_2 = "\u001B[33m";
    private static final String ANSI_SERVER = "\u001B[90m";
    private static final String ANSI_DEFAULT_SIMULATION = "\u001B[37m";

    private static final TerminalUiState INSTANCE = new TerminalUiState();
    private static final boolean ANSI_ENABLED = System.console() != null;

    private final Map<ClientId, ClientDisplay> displaysByClientId = new HashMap<>();
    private final Map<SimulationId, String> colorsBySimulationId = new HashMap<>();
    private ClientId selectedClientId;

    private TerminalUiState() {
    }

    public static TerminalUiState getInstance() {
        return INSTANCE;
    }

    public synchronized void registerClient(ClientId clientId, SimulationId simulationId, String color) {
        ClientId validatedClientId = Objects.requireNonNull(clientId, "clientId");
        SimulationId validatedSimulationId = Objects.requireNonNull(simulationId, "simulationId");
        String validatedColor = requireNonBlank(color, "color");

        displaysByClientId.put(validatedClientId, new ClientDisplay(validatedSimulationId, validatedColor));
        colorsBySimulationId.put(validatedSimulationId, validatedColor);

        if (selectedClientId == null) {
            selectedClientId = validatedClientId;
        }
    }

    public synchronized boolean selectClient(String rawSelection) {
        String selection = requireNonBlank(rawSelection, "rawSelection").trim();
        for (ClientId clientId : displaysByClientId.keySet()) {
            if (clientId.value().equalsIgnoreCase(selection) || clientId.value().endsWith(selection)) {
                selectedClientId = clientId;
                return true;
            }
        }

        return false;
    }

    public synchronized ClientId getSelectedClientId() {
        if (selectedClientId == null) {
            throw new IllegalStateException("Selected client must exist before use");
        }

        return selectedClientId;
    }

    public synchronized String prompt() {
        ClientId clientId = getSelectedClientId();
        ClientDisplay display = requireDisplay(clientId);
        return "Input ["
            + formatClient(clientId)
            + " -> "
            + formatSimulation(display.simulationId)
            + "] Enter=tick, "
            + TerminalCommands.INCREMENT_COMMAND
            + "/"
            + TerminalCommands.DECREMENT_COMMAND
            + "=apply, "
            + TerminalCommands.SERVER_DIRECTION_USAGE
            + ", "
            + TerminalCommands.CLIENT_SWITCH_USAGE
            + "=switch, "
            + TerminalCommands.ADD_SIMULATION_COMMAND
            + "=server, "
            + TerminalCommands.EXIT_COMMAND
            + "=quit: ";
    }

    public synchronized String formatClient(ClientId clientId) {
        ClientId validatedClientId = Objects.requireNonNull(clientId, "clientId");
        ClientDisplay display = displaysByClientId.get(validatedClientId);
        if (display == null) {
            return colorize(validatedClientId.value(), ANSI_DEFAULT_SIMULATION);
        }

        return colorize(validatedClientId.value(), display.color);
    }

    public synchronized String formatSimulation(SimulationId simulationId) {
        SimulationId validatedSimulationId = Objects.requireNonNull(simulationId, "simulationId");
        return colorize(
            validatedSimulationId.value(),
            colorsBySimulationId.getOrDefault(validatedSimulationId, ANSI_DEFAULT_SIMULATION));
    }

    public synchronized String formatSimulationThreadTag(SimulationId simulationId) {
        SimulationId validatedSimulationId = Objects.requireNonNull(simulationId, "simulationId");
        String rawTag = "[name: " + validatedSimulationId.value() + "]";
        return colorize(rawTag, colorsBySimulationId.getOrDefault(validatedSimulationId, ANSI_DEFAULT_SIMULATION));
    }

    public String formatServerLabel() {
        return colorize("Server", ANSI_SERVER);
    }

    public static String client1Color() {
        return ANSI_CLIENT_1;
    }

    public static String client2Color() {
        return ANSI_CLIENT_2;
    }

    private ClientDisplay requireDisplay(ClientId clientId) {
        ClientDisplay display = displaysByClientId.get(clientId);
        if (display == null) {
            throw new IllegalStateException("Client must be registered before formatting: " + clientId);
        }

        return display;
    }

    private static String colorize(String value, String color) {
        if (!ANSI_ENABLED) {
            return value;
        }

        return color + value + ANSI_RESET;
    }

    private static String requireNonBlank(String value, String label) {
        String validatedValue = Objects.requireNonNull(value, label);
        if (validatedValue.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }

        return validatedValue;
    }

    private static final class ClientDisplay {
        private final SimulationId simulationId;
        private final String color;

        private ClientDisplay(SimulationId simulationId, String color) {
            this.simulationId = simulationId;
            this.color = color;
        }
    }
}