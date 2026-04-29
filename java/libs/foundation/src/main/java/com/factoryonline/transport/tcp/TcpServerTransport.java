package com.factoryonline.transport.tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.factoryonline.foundation.ids.ClientId;
import com.factoryonline.transport.ServerTransport;
import com.factoryonline.transport.TransportMessage;
import com.factoryonline.transport.commands.ClientTransportCommand;
import com.factoryonline.transport.kryo.KryoStreams;

public final class TcpServerTransport implements ServerTransport, AutoCloseable {
    private final ServerSocket serverSocket;
    private final Thread acceptThread;
    private final List<ClientTransportCommand> queuedMessages = new ArrayList<>();
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
    public List<ClientTransportCommand> drainMessages() {
        synchronized (queuedMessages) {
            List<ClientTransportCommand> drainedMessages = List.copyOf(queuedMessages);
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

    private void enqueue(ClientTransportCommand message) {
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
                throw new IllegalArgumentException("Unknown client: " + validatedClientId.value);
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
        private final Kryo kryo;
        private final Input input;
        private final Output output;
        private final Thread readerThread;
        private final Object writeMonitor = new Object();

        private volatile boolean connectionClosed;
        private volatile ClientId clientId;

        private ClientConnection(Socket socket) throws IOException {
            this.socket = Objects.requireNonNull(socket, "socket");

            Input openedInput = null;
            Output openedOutput = null;
            IOException initializationFailure = null;
            try {
                openedInput = new Input(socket.getInputStream());
                openedOutput = new Output(socket.getOutputStream());
            } catch (IOException exception) {
                initializationFailure = exception;
            }

            if (initializationFailure != null) {
                closeQuietly(openedInput);
                closeQuietly(openedOutput);
                this.socket.close();
                throw initializationFailure;
            }

            this.kryo = KryoStreams.createKryo();
            this.input = openedInput;
            this.output = openedOutput;
            this.readerThread = new Thread(this::readLoop, "tcp-server-client-reader-" + socket.getPort());
            this.readerThread.setDaemon(true);
        }

        private void start() {
            readerThread.start();
        }

        private void readLoop() {
            try {
                while (!connectionClosed) {
                    Object decodedObject = kryo.readClassAndObject(input);
                    if (!(decodedObject instanceof ClientTransportCommand)) {
                        throw new IllegalStateException("Server transport received non-client payload: " + decodedObject);
                    }

                    ClientTransportCommand message = (ClientTransportCommand) decodedObject;
                    clientId = message.clientId;
                    registerConnection(clientId, this);
                    enqueue(message);
                }
            } catch (KryoException exception) {
                if (!connectionClosed && !closed && !isEndOfStream(exception)) {
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
                    throw new IllegalStateException("Connection is closed for client " + clientId.value);
                }

                try {
                    kryo.writeClassAndObject(output, Objects.requireNonNull(message, "message").payload);
                    output.flush();
                } catch (KryoException exception) {
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

            input.close();
            output.close();

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

    private static boolean isEndOfStream(KryoException exception) {
        String message = exception.getMessage();
        return message != null && message.contains("Buffer underflow");
    }
}
