package com.factoryonline.transport.local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.protocol.InitialSimulationState;
import com.factoryonline.foundation.protocol.InitialSimulationStateDTO;
import com.factoryonline.foundation.protocol.JoinSimulationRequest;
import com.factoryonline.foundation.protocol.JoinSimulationRequestDTO;
import com.factoryonline.foundation.protocol.ProtocolDTO;
import com.factoryonline.foundation.protocol.ProtocolDTOContainer;
import com.factoryonline.foundation.protocol.SimulationInputRequest;
import com.factoryonline.foundation.protocol.SimulationInputRequestDTO;
import com.factoryonline.foundation.protocol.SimulationUpdate;
import com.factoryonline.foundation.protocol.SimulationUpdateDTO;

public final class LocalTransportHub {
    private final int transportDelayTicks;
    private final Map<ClientId, ClientInbox> clientInboxes = new HashMap<>();
    private final List<ScheduledValue<String>> scheduledJoinRequests = new ArrayList<>();
    private final List<ScheduledValue<String>> scheduledSimulationInputRequests = new ArrayList<>();
    private int currentTick;

    public LocalTransportHub() {
        this(0);
    }

    public LocalTransportHub(int transportDelayTicks) {
        if (transportDelayTicks < 0) {
            throw new IllegalArgumentException("transportDelayTicks must not be negative");
        }

        this.transportDelayTicks = transportDelayTicks;
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

    synchronized void sendJoinRequest(JoinSimulationRequestDTO joinRequest) {
        scheduledJoinRequests.add(new ScheduledValue<>(
            Objects.requireNonNull(joinRequest, "joinRequest").serialize(),
            currentTick));
    }

    synchronized List<JoinSimulationRequest> drainJoinRequests() {
        List<JoinSimulationRequest> joinRequests = new ArrayList<>();
        for (String serializedDto : drainReady(scheduledJoinRequests)) {
            joinRequests.add(deserializeJoinRequest(serializedDto));
        }

        return joinRequests;
    }

    synchronized void sendSimulationInputRequest(SimulationInputRequestDTO simulationInputRequest) {
        scheduledSimulationInputRequests.add(new ScheduledValue<>(
            Objects.requireNonNull(simulationInputRequest, "simulationInputRequest").serialize(),
            currentTick + transportDelayTicks));
    }

    synchronized List<SimulationInputRequest> drainSimulationInputRequests() {
        List<SimulationInputRequest> inputRequests = new ArrayList<>();
        for (String serializedDto : drainReady(scheduledSimulationInputRequests)) {
            inputRequests.add(deserializeSimulationInputRequest(serializedDto));
        }

        return inputRequests;
    }

    synchronized void sendInitialState(ClientId clientId, InitialSimulationStateDTO initialState) {
        ClientInbox inbox = requireClientInbox(clientId);
        inbox.scheduledInitialStates.add(new ScheduledValue<>(
            Objects.requireNonNull(initialState, "initialState").serialize(),
            currentTick + transportDelayTicks));
    }

    synchronized List<InitialSimulationState> drainInitialStates(ClientId clientId) {
        List<InitialSimulationState> initialStates = new ArrayList<>();
        for (String serializedDto : drainReady(requireClientInbox(clientId).scheduledInitialStates)) {
            initialStates.add(deserializeInitialState(serializedDto));
        }

        return initialStates;
    }

    synchronized void sendSimulationUpdate(ClientId clientId, SimulationUpdateDTO simulationUpdate) {
        ClientInbox inbox = requireClientInbox(clientId);
        inbox.scheduledSimulationUpdates.add(new ScheduledValue<>(
            Objects.requireNonNull(simulationUpdate, "simulationUpdate").serialize(),
            currentTick + transportDelayTicks));
    }

    synchronized List<SimulationUpdate> drainSimulationUpdates(ClientId clientId) {
        List<SimulationUpdate> simulationUpdates = new ArrayList<>();
        for (String serializedDto : drainReady(requireClientInbox(clientId).scheduledSimulationUpdates)) {
            simulationUpdates.add(deserializeSimulationUpdate(serializedDto));
        }

        return simulationUpdates;
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

    private JoinSimulationRequest deserializeJoinRequest(String serializedDto) {
        ProtocolDTOContainer dto = ProtocolDTO.deserialize(serializedDto);
        switch (dto.getId().value()) {
            case JoinSimulationRequestDTO.ID_VALUE:
                return JoinSimulationRequestDTO.from(dto.getData());
            default:
                throw new IllegalArgumentException("Unexpected join request DTO id: " + dto.getId());
        }
    }

    private SimulationInputRequest deserializeSimulationInputRequest(String serializedDto) {
        ProtocolDTOContainer dto = ProtocolDTO.deserialize(serializedDto);
        switch (dto.getId().value()) {
            case SimulationInputRequestDTO.ID_VALUE:
                return SimulationInputRequestDTO.from(dto.getData());
            default:
                throw new IllegalArgumentException("Unexpected simulation input DTO id: " + dto.getId());
        }
    }

    private InitialSimulationState deserializeInitialState(String serializedDto) {
        ProtocolDTOContainer dto = ProtocolDTO.deserialize(serializedDto);
        switch (dto.getId().value()) {
            case InitialSimulationStateDTO.ID_VALUE:
                return InitialSimulationStateDTO.from(dto.getData());
            default:
                throw new IllegalArgumentException("Unexpected initial state DTO id: " + dto.getId());
        }
    }

    private SimulationUpdate deserializeSimulationUpdate(String serializedDto) {
        ProtocolDTOContainer dto = ProtocolDTO.deserialize(serializedDto);
        switch (dto.getId().value()) {
            case SimulationUpdateDTO.ID_VALUE:
                return SimulationUpdateDTO.from(dto.getData());
            default:
                throw new IllegalArgumentException("Unexpected simulation update DTO id: " + dto.getId());
        }
    }

    private static final class ClientInbox {
        private final List<ScheduledValue<String>> scheduledInitialStates = new ArrayList<>();
        private final List<ScheduledValue<String>> scheduledSimulationUpdates = new ArrayList<>();
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