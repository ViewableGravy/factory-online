package com.factoryonline.transport.local;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.protocol.ClientTransportMessageDTO;
import com.factoryonline.foundation.protocol.ProtocolDTO;
import com.factoryonline.foundation.protocol.ProtocolDTOContainer;

public final class LocalTransportHub {
    private final int transportDelayTicks;
    private final Map<ClientId, ClientInbox> clientInboxes = new HashMap<>();
    private final MessageQueue serverInbox = new MessageQueue();
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

    synchronized void sendToServer(ClientId clientId, ProtocolDTO<?> dto, boolean delayed) {
        serverInbox.schedule(
            new ClientTransportMessageDTO(
                Objects.requireNonNull(clientId, "clientId"),
                Objects.requireNonNull(dto, "dto").toContainer()),
            currentTick + (delayed ? transportDelayTicks : 0));
    }

    synchronized <T, D extends ProtocolDTO<T>> List<T> drainServerAs(Class<D> dtoClass) {
        return serverInbox.drainAs(dtoClass, currentTick);
    }

    synchronized void sendToClient(ClientId clientId, ProtocolDTO<?> dto) {
        ClientInbox inbox = requireClientInbox(clientId);
        inbox.messageQueue.schedule(
            Objects.requireNonNull(dto, "dto"),
            currentTick + transportDelayTicks);
    }

    synchronized <T, D extends ProtocolDTO<T>> List<T> drainClientAs(ClientId clientId, Class<D> dtoClass) {
        return requireClientInbox(clientId).messageQueue.drainAs(dtoClass, currentTick);
    }

    private ClientInbox requireClientInbox(ClientId clientId) {
        ClientId validatedClientId = Objects.requireNonNull(clientId, "clientId");
        ClientInbox inbox = clientInboxes.get(validatedClientId);
        if (inbox == null) {
            throw new IllegalArgumentException("Unknown client: " + validatedClientId);
        }

        return inbox;
    }

    private static final class ClientInbox {
        private final MessageQueue messageQueue = new MessageQueue();
    }

    private static final class MessageQueue {
        private final List<ScheduledValue<ProtocolDTOContainer>> scheduledDtos = new ArrayList<>();
        private final Map<String, List<ProtocolDTOContainer>> queuedDtosById = new HashMap<>();

        private void schedule(ProtocolDTO<?> dto, int deliveryTick) {
            scheduledDtos.add(new ScheduledValue<>(dto.toContainer(), deliveryTick));
        }

        private <T, D extends ProtocolDTO<T>> List<T> drainAs(Class<D> dtoClass, int currentTick) {
            drain(currentTick);

            String dtoId = ProtocolDTO.resolveId(dtoClass).value();
            List<ProtocolDTOContainer> queuedDtos = queuedDtosById.remove(dtoId);
            if (queuedDtos == null || queuedDtos.isEmpty()) {
                return List.of();
            }

            List<T> drainedValues = new ArrayList<>(queuedDtos.size());
            for (ProtocolDTOContainer queuedDto : queuedDtos) {
                drainedValues.add(ProtocolDTO.fromContainer(dtoClass, queuedDto));
            }

            return drainedValues;
        }

        private void drain(int currentTick) {
            Iterator<ScheduledValue<ProtocolDTOContainer>> iterator = scheduledDtos.iterator();

            while (iterator.hasNext()) {
                ScheduledValue<ProtocolDTOContainer> scheduledDto = iterator.next();
                if (scheduledDto.deliveryTick > currentTick) {
                    continue;
                }

                queuedDtosById
                    .computeIfAbsent(scheduledDto.value.getId().value(), ignored -> new ArrayList<>())
                    .add(scheduledDto.value);
                iterator.remove();
            }
        }
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