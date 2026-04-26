package com.factoryonline.transport.tcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.protocol.ClientTransportMessageDTO;
import com.factoryonline.foundation.protocol.ProtocolDTO;
import com.factoryonline.foundation.protocol.ProtocolDTOContainer;
import com.factoryonline.transport.ClientTransport;
import com.factoryonline.transport.TransportMessage;

public final class TcpClientTransport implements ClientTransport, AutoCloseable {
    private final ClientId clientId;
    private final Socket socket;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final Thread readerThread;
    private final AtomicInteger currentTick = new AtomicInteger();
    private final Object writeMonitor = new Object();
    private final List<ProtocolDTOContainer> queuedDtos = new ArrayList<>();

    private volatile boolean closed;

    public TcpClientTransport(String host, int port, ClientId clientId) throws IOException {
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        this.socket = new Socket(requireNonBlank(host, "host"), requirePort(port));
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        this.readerThread = new Thread(this::readLoop, "tcp-client-transport-reader-" + clientId.value());
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
        writeLine(new ClientTransportMessageDTO(clientId, message.getPayload()).serialize());
    }

    @Override
    public int getCurrentTick() {
        return currentTick.get();
    }

    public void advanceTick() {
        currentTick.incrementAndGet();
    }

    @Override
    public <T, D extends ProtocolDTO<T>> List<T> drainAs(Class<D> dtoClass) {
        Objects.requireNonNull(dtoClass, "dtoClass");

        List<T> drainedValues = new ArrayList<>();
        String dtoId = ProtocolDTO.resolveId(dtoClass).value();
        synchronized (queuedDtos) {
            Iterator<ProtocolDTOContainer> iterator = queuedDtos.iterator();
            while (iterator.hasNext()) {
                ProtocolDTOContainer queuedDto = iterator.next();
                if (!dtoId.equals(queuedDto.getId().value())) {
                    continue;
                }

                drainedValues.add(ProtocolDTO.fromContainer(dtoClass, queuedDto));
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

        try {
            reader.close();
        } catch (IOException exception) {
            if (failure == null) {
                failure = exception;
            } else {
                failure.addSuppressed(exception);
            }
        }

        try {
            writer.close();
        } catch (IOException exception) {
            if (failure == null) {
                failure = exception;
            } else {
                failure.addSuppressed(exception);
            }
        }

        if (failure != null) {
            throw failure;
        }
    }

    private void readLoop() {
        try {
            String line;
            while (!closed && (line = reader.readLine()) != null) {
                ProtocolDTOContainer container = ProtocolDTO.deserialize(line);
                synchronized (queuedDtos) {
                    queuedDtos.add(container);
                }
            }
        } catch (IOException exception) {
            if (!closed) {
                System.err.println("Client transport read failed: " + exception.getMessage());
            }
        }
    }

    private void writeLine(String line) {
        synchronized (writeMonitor) {
            if (closed) {
                throw new IllegalStateException("Transport is closed for client " + clientId.value());
            }

            try {
                writer.write(line);
                writer.newLine();
                writer.flush();
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to send client transport message", exception);
            }
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
}