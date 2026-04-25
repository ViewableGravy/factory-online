package com.factoryonline.transport.local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;

public final class LocalTransportHub {
    private final int initialStateDelayTicks;
    private final Map<ClientId, ClientInbox> clientInboxes = new HashMap<>();
    private final List<ScheduledValue<JoinSimulationRequest>> scheduledJoinRequests = new ArrayList<>();
    private final List<ScheduledValue<SimulationInputRequest>> scheduledSimulationInputRequests = new ArrayList<>();
    private int currentTick;

    public LocalTransportHub() {
        this(0);
    }

    public LocalTransportHub(int initialStateDelayTicks) {
        if (initialStateDelayTicks < 0) {
            throw new IllegalArgumentException("initialStateDelayTicks must not be negative");
        }

        this.initialStateDelayTicks = initialStateDelayTicks;
    }

    public synchronized LocalServerTransport createServerTransport() {
        return new LocalServerTransport(this);
    }

    public synchronized LocalClientTransport createClientTransport(ClientId clientId) {
        ClientId validatedClientId = Objects.requireNonNull(clientId, "clientId");
        clientInboxes.computeIfAbsent(validatedClientId, ignored -> new ClientInbox());
        return new LocalClientTransport(this, validatedClientId);
    }

    public synchronized void advanceTick() {
        currentTick += 1;
    }

    public synchronized int getCurrentTick() {
        return currentTick;
    }

    synchronized void sendJoinRequest(JoinSimulationRequest joinRequest) {
        scheduledJoinRequests.add(new ScheduledValue<>(Objects.requireNonNull(joinRequest, "joinRequest"), currentTick));
    }

    synchronized List<JoinSimulationRequest> drainJoinRequests() {
        return drainReady(scheduledJoinRequests);
    }

    synchronized void sendSimulationInputRequest(SimulationInputRequest simulationInputRequest) {
        scheduledSimulationInputRequests.add(new ScheduledValue<>(
            Objects.requireNonNull(simulationInputRequest, "simulationInputRequest"),
            currentTick));
    }

    synchronized List<SimulationInputRequest> drainSimulationInputRequests() {
        return drainReady(scheduledSimulationInputRequests);
    }

    synchronized void sendInitialState(ClientId clientId, InitialSimulationState initialState) {
        ClientInbox inbox = requireClientInbox(clientId);
        inbox.scheduledInitialStates.add(new ScheduledValue<>(
            Objects.requireNonNull(initialState, "initialState"),
            currentTick + initialStateDelayTicks));
    }

    synchronized List<InitialSimulationState> drainInitialStates(ClientId clientId) {
        return drainReady(requireClientInbox(clientId).scheduledInitialStates);
    }

    synchronized void sendSimulationUpdate(ClientId clientId, SimulationUpdate simulationUpdate) {
        ClientInbox inbox = requireClientInbox(clientId);
        inbox.scheduledSimulationUpdates.add(new ScheduledValue<>(
            Objects.requireNonNull(simulationUpdate, "simulationUpdate"),
            currentTick));
    }

    synchronized List<SimulationUpdate> drainSimulationUpdates(ClientId clientId) {
        return drainReady(requireClientInbox(clientId).scheduledSimulationUpdates);
    }

    private ClientInbox requireClientInbox(ClientId clientId) {
        ClientId validatedClientId = Objects.requireNonNull(clientId, "clientId");
        ClientInbox inbox = clientInboxes.get(validatedClientId);
        if (inbox == null) {
            throw new IllegalArgumentException("Unknown client: " + validatedClientId);
        }

        return inbox;
    }

    private <T> List<T> drainReady(List<ScheduledValue<T>> scheduledValues) {
        List<T> readyValues = new ArrayList<>();
        Iterator<ScheduledValue<T>> iterator = scheduledValues.iterator();

        while (iterator.hasNext()) {
            ScheduledValue<T> scheduledValue = iterator.next();
            if (scheduledValue.deliveryTick > currentTick) {
                continue;
            }

            readyValues.add(scheduledValue.value);
            iterator.remove();
        }

        return readyValues;
    }

    private static final class ClientInbox {
        private final List<ScheduledValue<InitialSimulationState>> scheduledInitialStates = new ArrayList<>();
        private final List<ScheduledValue<SimulationUpdate>> scheduledSimulationUpdates = new ArrayList<>();
    }

    private static final class ScheduledValue<T> {
        private final T value;
        private final int deliveryTick;

        private ScheduledValue(T value, int deliveryTick) {
            this.value = value;
            this.deliveryTick = deliveryTick;
        }
    }
}