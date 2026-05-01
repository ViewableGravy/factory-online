package com.factoryonline.client.scheduler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.config.RuntimeTiming;
import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.foundation.timing.TickControl;
import com.factoryonline.server.bootstrap.TerminalUiState;
import com.factoryonline.transport.commands.TickSyncCommand;

public final class ClientTickSynchronizer {
    private static final TerminalUiState TERMINAL_UI_STATE = TerminalUiState.getInstance();

    private final ClientId clientId;
    private final Map<SimulationId, TickSyncState> statesBySimulation = new HashMap<>();
    private TickControl tickControl = TickControl.automatic(RuntimeTiming.TICK_INTERVAL_MILLIS);

    public ClientTickSynchronizer(ClientId clientId) {
        this.clientId = Objects.requireNonNull(clientId, "clientId");
    }

    public void receive(TickSyncCommand tickSyncMessage, int observedAtTransportTick) {
        TickSyncCommand validatedMessage = Objects.requireNonNull(tickSyncMessage, "tickSyncMessage");
        TickSyncState previousState = statesBySimulation.get(validatedMessage.simulationId);
        double pacingAdjustmentCredit = previousState == null ? 0.0D : previousState.pacingAdjustmentCredit;

        statesBySimulation.put(
            validatedMessage.simulationId,
            new TickSyncState(
                validatedMessage.serverTick,
                observedAtTransportTick,
                pacingAdjustmentCredit,
                validatedMessage.tickControl));
        tickControl = validatedMessage.tickControl;
    }

    public TickControl getTickControl() {
        return tickControl;
    }

    public int requestTicks(
        SimulationId activeSimulationId,
        int localTick,
        int currentTransportTick,
        boolean automaticTickDue
    ) {
        if (activeSimulationId == null) {
            return automaticTickDue ? 1 : 0;
        }

        TickSyncState tickSyncState = statesBySimulation.get(activeSimulationId);
        if (tickSyncState == null) {
            return automaticTickDue ? 1 : 0;
        }

        if (tickSyncState.tickControl.isManual()) {
            return Math.max(0, tickSyncState.serverTick - localTick);
        }

        // Incoming transport messages can wake the client loop between cadence ticks.
        // Keep those wakeups read-only so lag correction uses the extrapolated server time,
        // not the raw just-received tick snapshot.
        if (!automaticTickDue) {
            return 0;
        }

        int estimatedServerTick = tickSyncState.estimateCurrentServerTick(currentTransportTick);
        int targetLagTicks = RuntimeTiming.CLIENT_TARGET_LOCAL_BUFFER_TICKS;
        int currentLagTicks = estimatedServerTick - localTick;
        int lagErrorTicks = currentLagTicks - targetLagTicks;

        if (lagErrorTicks < -RuntimeTiming.CLIENT_HARD_CORRECTION_TICKS) {
            System.out.println(
                "Client " + TERMINAL_UI_STATE.formatClient(clientId)
                    + " is ahead of the server for " + TERMINAL_UI_STATE.formatSimulation(activeSimulationId)
                    + " (current lag " + currentLagTicks + " ticks, target " + targetLagTicks
                    + " +/- " + RuntimeTiming.CLIENT_HARD_CORRECTION_TICKS + "); holding local simulation");
            return 0;
        }

        if (lagErrorTicks > RuntimeTiming.CLIENT_HARD_CORRECTION_TICKS) {
            System.out.println(
                "Client " + TERMINAL_UI_STATE.formatClient(clientId)
                    + " is behind the server for " + TERMINAL_UI_STATE.formatSimulation(activeSimulationId)
                    + " (current lag " + currentLagTicks + " ticks, target " + targetLagTicks
                    + " +/- " + RuntimeTiming.CLIENT_HARD_CORRECTION_TICKS + "); running catch-up ticks");
            return RuntimeTiming.CLIENT_CATCH_UP_TICKS;
        }

        tickSyncState.pacingAdjustmentCredit += lagErrorTicks * RuntimeTiming.CLIENT_RATE_ADJUSTMENT_GAIN;
        if (tickSyncState.pacingAdjustmentCredit <= -1.0D
            && lagErrorTicks < -RuntimeTiming.CLIENT_LAG_TOLERANCE_TICKS) {
            tickSyncState.pacingAdjustmentCredit += 1.0D;
            return 0;
        }

        if (tickSyncState.pacingAdjustmentCredit >= 1.0D
            && lagErrorTicks > RuntimeTiming.CLIENT_LAG_TOLERANCE_TICKS) {
            tickSyncState.pacingAdjustmentCredit -= 1.0D;
            return RuntimeTiming.CLIENT_CATCH_UP_TICKS;
        }

        return automaticTickDue ? 1 : 0;
    }

    private static final class TickSyncState {
        private final int serverTick;
        private final int observedAtTransportTick;
        private final TickControl tickControl;
        private double pacingAdjustmentCredit;

        private TickSyncState(
            int serverTick,
            int observedAtTransportTick,
            double pacingAdjustmentCredit,
            TickControl tickControl
        ) {
            this.serverTick = serverTick;
            this.observedAtTransportTick = observedAtTransportTick;
            this.pacingAdjustmentCredit = pacingAdjustmentCredit;
            this.tickControl = Objects.requireNonNull(tickControl, "tickControl");
        }

        private int estimateCurrentServerTick(int currentTransportTick) {
            return serverTick + Math.max(0, currentTransportTick - observedAtTransportTick);
        }
    }
}
