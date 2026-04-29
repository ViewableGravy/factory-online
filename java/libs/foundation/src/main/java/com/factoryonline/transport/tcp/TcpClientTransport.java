package com.factoryonline.transport.tcp;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.transport.commands.ClientTransportCommand;
import com.factoryonline.transport.commands.ProtocolCommand;
import com.factoryonline.transport.ClientTransport;
import com.factoryonline.transport.TransportMessage;
import com.factoryonline.transport.kryo.KryoStreams;

public final class TcpClientTransport implements ClientTransport, AutoCloseable {
    private final ClientId clientId;
    private final Socket socket;
    private final Kryo kryo;
    private final Input input;
    private final Output output;
    private final Thread readerThread;
    private final AtomicInteger currentTick = new AtomicInteger();
    private final Object writeMonitor = new Object();
    private final List<ProtocolCommand> queuedCommands = new ArrayList<>();
    private final List<Runnable> messageListeners = new ArrayList<>();

    private volatile boolean closed;

    public TcpClientTransport(String host, int port, ClientId clientId) throws IOException {
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.socket = new Socket(requireNonBlank(host, "host"), requirePort(port));
        this.kryo = KryoStreams.createKryo();
        this.input = new Input(socket.getInputStream());
        this.output = new Output(socket.getOutputStream());
        this.readerThread = new Thread(this::readLoop, "tcp-client-transport-reader-" + clientId.value);
        this.readerThread.setDaemon(true);
        this.readerThread.start();
    }

    @Override
    public ClientId getClientId() {
        return clientId;
    }

    @Override
    public void send(TransportMessage message, boolean delayed) {
        Objects.requireNonNull(message, "message");
        writeCommand(new ClientTransportCommand(clientId, message.payload));
    }

    @Override
    public int getCurrentTick() {
        return currentTick.get();
    }

    @Override
    public void advanceTick() {
        currentTick.incrementAndGet();
    }

    @Override
    public void addMessageListener(Runnable listener) {
        synchronized (messageListeners) {
            messageListeners.add(Objects.requireNonNull(listener, "listener"));
        }
    }

    @Override
    public <T extends ProtocolCommand> List<T> drainAs(Class<T> commandClass) {
        Objects.requireNonNull(commandClass, "commandClass");

        List<T> drainedValues = new ArrayList<>();
        synchronized (queuedCommands) {
            Iterator<ProtocolCommand> iterator = queuedCommands.iterator();
            while (iterator.hasNext()) {
                ProtocolCommand queuedDto = iterator.next();
                if (!commandClass.isInstance(queuedDto)) {
                    continue;
                }

                drainedValues.add(commandClass.cast(queuedDto));
                iterator.remove();
            }
        }

        return drainedValues;
    }

    @Override
    public void close() throws IOException {
        closed = true;
        IOException failure = null;

        try {
            socket.close();
        } catch (IOException exception) {
            failure = exception;
        }

        input.close();
        output.close();

        if (failure != null) {
            throw failure;
        }
    }

    private void readLoop() {
        try {
            while (!closed) {
                Object decodedObject = kryo.readClassAndObject(input);
                if (!(decodedObject instanceof ProtocolCommand)) {
                    throw new IllegalStateException("Client transport received non-command payload: " + decodedObject);
                }

                ProtocolCommand container = (ProtocolCommand) decodedObject;
                synchronized (queuedCommands) {
                    queuedCommands.add(container);
                }
                notifyMessageListeners();
            }
        } catch (KryoException exception) {
            if (!closed && !isEndOfStream(exception)) {
                System.err.println("Client transport read failed: " + exception.getMessage());
            }
        }
    }

    private void writeCommand(ProtocolCommand command) {
        synchronized (writeMonitor) {
            if (closed) {
                throw new IllegalStateException("Transport is closed for client " + clientId.value);
            }

            try {
                kryo.writeClassAndObject(output, Objects.requireNonNull(command, "command"));
                output.flush();
            } catch (KryoException exception) {
                throw new IllegalStateException("Failed to send client transport message", exception);
            }
        }
    }

    private void notifyMessageListeners() {
        List<Runnable> listeners;
        synchronized (messageListeners) {
            listeners = List.copyOf(messageListeners);
        }

        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    private static String requireNonBlank(String value, String label) {
        String validatedValue = Objects.requireNonNull(value, label);
        if (validatedValue.isBlank()) {
            throw new IllegalArgumentException(label + " must not be blank");
        }

        return validatedValue;
    }

    private static int requirePort(int port) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }

        return port;
    }

    private static boolean isEndOfStream(KryoException exception) {
        String message = exception.getMessage();
        return message != null && message.contains("Buffer underflow");
    }
}
