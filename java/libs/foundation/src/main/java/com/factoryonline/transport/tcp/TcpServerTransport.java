package com.factoryonline.transport.tcp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.foundation.protocol.ClientTransportMessage;
import com.factoryonline.foundation.protocol.ClientTransportMessageDTO;
import com.factoryonline.foundation.protocol.ProtocolDTO;
import com.factoryonline.transport.ServerTransport;
import com.factoryonline.transport.TransportMessage;

public final class TcpServerTransport implements ServerTransport, AutoCloseable {
    private final ServerSocket serverSocket;
    private final Thread acceptThread;
    private final List<ClientTransportMessage> queuedMessages = new ArrayList<>();
    private final Map<ClientId, ClientConnection> connectionsByClientId = new HashMap<>();
    private final List<Runnable> messageListeners = new ArrayList<>();

    private volatile boolean closed;

    public TcpServerTransport(int port) throws IOException {
        this.serverSocket = new ServerSocket(requirePort(port));
        this.acceptThread = new Thread(this::acceptLoop, "tcp-server-transport-accept");
        this.acceptThread.setDaemon(true);
        this.acceptThread.start();
    }

    @Override
    public List<ClientTransportMessage> drainMessages() {
        synchronized (queuedMessages) {
            List<ClientTransportMessage> drainedMessages = List.copyOf(queuedMessages);
            queuedMessages.clear();
            return drainedMessages;
        }
    }

    @Override
    public void addMessageListener(Runnable listener) {
        synchronized (messageListeners) {
            messageListeners.add(Objects.requireNonNull(listener, "listener"));
        }
    }

    @Override
    public void send(ClientId clientId, TransportMessage message) {
        Objects.requireNonNull(message, "message");
        requireConnection(clientId).send(message);
    }

    @Override
    public void close() throws IOException {
        closed = true;
        IOException failure = null;

        try {
            serverSocket.close();
        } catch (IOException exception) {
            failure = exception;
        }

        List<ClientConnection> connections;
        synchronized (connectionsByClientId) {
            connections = List.copyOf(connectionsByClientId.values());
            connectionsByClientId.clear();
        }

        for (ClientConnection connection : connections) {
            try {
                connection.close();
            } catch (IOException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }

        if (failure != null) {
            throw failure;
        }
    }

    @SuppressWarnings("resource")
    private void acceptLoop() {
        while (!closed) {
            try {
                Socket socket = serverSocket.accept();
                ClientConnection connection = new ClientConnection(socket);
                try {
                    connection.start();
                } catch (RuntimeException exception) {
                    connection.close();
                    throw exception;
                }
            } catch (SocketException exception) {
                if (!closed) {
                    System.err.println("Server transport accept failed: " + exception.getMessage());
                }
                return;
            } catch (IOException exception) {
                if (!closed) {
                    System.err.println("Server transport accept failed: " + exception.getMessage());
                }
            }
        }
    }

    private void enqueue(ClientTransportMessage message) {
        synchronized (queuedMessages) {
            queuedMessages.add(message);
        }
        notifyMessageListeners();
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

    private void registerConnection(ClientId clientId, ClientConnection connection) {
        ClientConnection previousConnection;
        synchronized (connectionsByClientId) {
            previousConnection = connectionsByClientId.put(clientId, connection);
        }

        if (previousConnection != null && previousConnection != connection) {
            try {
                previousConnection.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void removeConnection(ClientId clientId, ClientConnection connection) {
        synchronized (connectionsByClientId) {
            ClientConnection currentConnection = connectionsByClientId.get(clientId);
            if (currentConnection == connection) {
                connectionsByClientId.remove(clientId);
            }
        }
    }

    private ClientConnection requireConnection(ClientId clientId) {
        ClientId validatedClientId = Objects.requireNonNull(clientId, "clientId");
        synchronized (connectionsByClientId) {
            ClientConnection connection = connectionsByClientId.get(validatedClientId);
            if (connection == null) {
                throw new IllegalArgumentException("Unknown client: " + validatedClientId.value());
            }

            return connection;
        }
    }

    private static int requirePort(int port) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }

        return port;
    }

    private final class ClientConnection implements AutoCloseable {
        private final Socket socket;
        private final BufferedReader reader;
        private final BufferedWriter writer;
        private final Thread readerThread;
        private final Object writeMonitor = new Object();

        private volatile boolean connectionClosed;
        private volatile ClientId clientId;

        private ClientConnection(Socket socket) throws IOException {
            this.socket = Objects.requireNonNull(socket, "socket");

            BufferedReader openedReader = null;
            BufferedWriter openedWriter = null;
            IOException initializationFailure = null;
            try {
                openedReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                openedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            } catch (IOException exception) {
                initializationFailure = exception;
            }

            if (initializationFailure != null) {
                closeQuietly(openedReader);
                closeQuietly(openedWriter);
                this.socket.close();
                throw initializationFailure;
            }

            this.reader = openedReader;
            this.writer = openedWriter;
            this.readerThread = new Thread(this::readLoop, "tcp-server-client-reader-" + socket.getPort());
            this.readerThread.setDaemon(true);
        }

        private void start() {
            readerThread.start();
        }

        private void readLoop() {
            try {
                String line;
                while (!connectionClosed && (line = reader.readLine()) != null) {
                    ClientTransportMessage message = ClientTransportMessageDTO.from(ProtocolDTO.deserialize(line).getData());
                    clientId = message.getClientId();
                    registerConnection(clientId, this);
                    enqueue(message);
                }
            } catch (IOException exception) {
                if (!connectionClosed && !closed) {
                    System.err.println("Server transport client read failed: " + exception.getMessage());
                }
            } finally {
                if (clientId != null) {
                    removeConnection(clientId, this);
                }

                try {
                    close();
                } catch (IOException ignored) {
                }
            }
        }

        private void send(TransportMessage message) {
            synchronized (writeMonitor) {
                if (connectionClosed) {
                    throw new IllegalStateException("Connection is closed for client " + clientId.value());
                }

                try {
                    writer.write(Objects.requireNonNull(message, "message").getPayload().serialize());
                    writer.newLine();
                    writer.flush();
                } catch (IOException exception) {
                    throw new IllegalStateException("Failed to send server transport message", exception);
                }
            }
        }

        @Override
        public void close() throws IOException {
            connectionClosed = true;
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

        private void closeQuietly(AutoCloseable closeable) {
            if (closeable == null) {
                return;
            }

            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }
}