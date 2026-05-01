package com.factoryonline.client.bootstrap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.factoryonline.foundation.config.RuntimeTiming;
import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.ids.SimulationId;
import com.factoryonline.foundation.timing.TickControl;
import com.factoryonline.simulation.Simulation;
import com.factoryonline.transport.ClientTransport;
import com.factoryonline.transport.TransportMessage;
import com.factoryonline.transport.commands.InitialSimulationStateCommand;
import com.factoryonline.transport.commands.ProtocolCommand;
import com.factoryonline.transport.commands.TickSyncCommand;

public final class ClientStartupBufferTest {
    private ClientStartupBufferTest() {
    }

    public static void main(String[] args) {
        keepsTheFullConfiguredStartupBuffer();
    }

    private static void keepsTheFullConfiguredStartupBuffer() {
        ClientId clientId = new ClientId("test-client");
        SimulationId simulationId = new SimulationId("test-simulation");
        FakeClientTransport transport = new FakeClientTransport(clientId);
        ClientApplication client = new ClientApplication(clientId, simulationId);

        client.initialize(transport);
        transport.queue(
            new InitialSimulationStateCommand(
                simulationId,
                new Simulation(simulationId).snapshot(),
                100));
        transport.queue(
            new TickSyncCommand(
                simulationId,
                100,
                0,
                TickControl.automatic(RuntimeTiming.TICK_INTERVAL_MILLIS)));

        client.processIncomingMessages();
        assertEquals(100, client.getLocalSimulationTick(), "client should start at the snapshot tick");

        for (int cycle = 1; cycle <= RuntimeTiming.CLIENT_TARGET_LOCAL_BUFFER_TICKS; cycle += 1) {
            transport.advanceTick();
            client.scheduleTicks(true);
            assertEquals(
                100,
                client.getLocalSimulationTick(),
                "client should hold the full startup buffer before running local ticks");
        }

        transport.advanceTick();
        client.scheduleTicks(true);
        assertEquals(101, client.getLocalSimulationTick(), "client should advance after the startup buffer is exhausted");

        client.cleanup();
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + " expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static final class FakeClientTransport implements ClientTransport {
        private final ClientId clientId;
        private final List<ProtocolCommand> queuedCommands = new ArrayList<>();
        private int currentTick;

        private FakeClientTransport(ClientId clientId) {
            this.clientId = clientId;
        }

        private void queue(ProtocolCommand command) {
            queuedCommands.add(command);
        }

        @Override
        public ClientId getClientId() {
            return clientId;
        }

        @Override
        public void send(TransportMessage message, boolean delayed) {
        }

        @Override
        public void advanceTick() {
            currentTick += 1;
        }

        @Override
        public void addMessageListener(Runnable listener) {
        }

        @Override
        public int getCurrentTick() {
            return currentTick;
        }

        @Override
        public <T extends ProtocolCommand> List<T> drainAs(Class<T> commandClass) {
            List<T> drainedValues = new ArrayList<>();
            Iterator<ProtocolCommand> iterator = queuedCommands.iterator();
            while (iterator.hasNext()) {
                ProtocolCommand queuedCommand = iterator.next();
                if (!commandClass.isInstance(queuedCommand)) {
                    continue;
                }

                drainedValues.add(commandClass.cast(queuedCommand));
                iterator.remove();
            }

            return drainedValues;
        }
    }
}