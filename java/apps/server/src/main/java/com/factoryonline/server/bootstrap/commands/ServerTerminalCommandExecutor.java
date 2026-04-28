package com.factoryonline.server.bootstrap.commands;

import java.util.Objects;

import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.foundation.timing.TickControl;
import com.factoryonline.server.bootstrap.ServerApplication;
import com.factoryonline.server.bootstrap.TerminalUiState;

public final class ServerTerminalCommandExecutor {
    private static final TerminalUiState TERMINAL_UI_STATE = TerminalUiState.getInstance();

    public void execute(ServerTerminalCommand command, ServerApplication server) {
        ServerTerminalCommand validatedCommand = Objects.requireNonNull(command, "command");
        ServerApplication validatedServer = Objects.requireNonNull(server, "server");

        if (validatedCommand instanceof ServerTerminalCommand.RequestSnapshot) {
            validatedServer.requestSnapshot();
            return;
        }

        if (validatedCommand instanceof ServerTerminalCommand.AddSimulation) {
            SimulationId simulationId = validatedServer.addSimulation();
            System.out.println(
                TERMINAL_UI_STATE.formatServerLabel()
                    + " added simulation "
                    + TERMINAL_UI_STATE.formatSimulation(simulationId)
                    + " in base state");
            return;
        }

        if (validatedCommand instanceof ServerTerminalCommand.QueueManualTicks) {
            ServerTerminalCommand.QueueManualTicks queueManualTicks = (ServerTerminalCommand.QueueManualTicks) validatedCommand;
            validatedServer.queueManualTicks(queueManualTicks.getCount());
            System.out.println(
                TERMINAL_UI_STATE.formatServerLabel()
                    + " queued "
                    + queueManualTicks.getCount()
                    + " manual tick"
                    + (queueManualTicks.getCount() == 1 ? "" : "s"));
            return;
        }

        if (validatedCommand instanceof ServerTerminalCommand.UpdateTickMode) {
            ServerTerminalCommand.UpdateTickMode updateTickMode = (ServerTerminalCommand.UpdateTickMode) validatedCommand;
            TickControl tickControl = validatedServer.setTickMode(updateTickMode.getTickMode());
            System.out.println(
                TERMINAL_UI_STATE.formatServerLabel()
                    + " switched tick mode to "
                    + tickControl.getMode().protocolValue()
                    + " at "
                    + tickControl.getTickIntervalMillis()
                    + " ms");
            return;
        }

        if (validatedCommand instanceof ServerTerminalCommand.UpdateTickRate) {
            ServerTerminalCommand.UpdateTickRate updateTickRate = (ServerTerminalCommand.UpdateTickRate) validatedCommand;
            TickControl tickControl = validatedServer.setTickIntervalMillis(updateTickRate.getTickIntervalMillis());
            System.out.println(
                TERMINAL_UI_STATE.formatServerLabel()
                    + " set tick interval to "
                    + tickControl.getTickIntervalMillis()
                    + " ms in "
                    + tickControl.getMode().protocolValue()
                    + " mode");
            return;
        }

        if (validatedCommand instanceof ServerTerminalCommand.ApplyServerSimulationInput) {
            ServerTerminalCommand.ApplyServerSimulationInput applyServerSimulationInput =
                (ServerTerminalCommand.ApplyServerSimulationInput) validatedCommand;
            validatedServer.queueServerSimulationInput(applyServerSimulationInput.getAugmentation());
            return;
        }

        throw new IllegalArgumentException(
            "Unhandled server terminal command type: " + validatedCommand.getClass().getName());
    }
}