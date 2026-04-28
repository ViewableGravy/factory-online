package com.factoryonline.client.bootstrap;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.factoryonline.client.bootstrap.commands.ClientTerminalCommand;
import com.factoryonline.client.bootstrap.commands.ClientTerminalCommandExecutor;
import com.factoryonline.client.bootstrap.commands.ClientTerminalCommandParser;
import com.factoryonline.client.bootstrap.commands.ClientTerminalCommandValidator;
import com.factoryonline.foundation.timing.TickControl;
import com.factoryonline.transport.ClientTransport;

public final class ClientRuntimeLoop {
    private final ClientApplication client;
    private final ClientTransport transport;
    private final Queue<String> queuedInputs = new ConcurrentLinkedQueue<>();
    private final Queue<ClientTerminalCommand> queuedCommands = new ConcurrentLinkedQueue<>();
    private final ClientTerminalCommandParser parser = new ClientTerminalCommandParser();
    private final ClientTerminalCommandValidator validator = new ClientTerminalCommandValidator();
    private final ClientTerminalCommandExecutor executor = new ClientTerminalCommandExecutor();
    private final Object wakeMonitor = new Object();
    private final AtomicBoolean running = new AtomicBoolean();
    private long wakeVersion;
    private Thread loopThread;

    public ClientRuntimeLoop(ClientApplication client, ClientTransport transport) {
        this.client = Objects.requireNonNull(client, "client");
        this.transport = Objects.requireNonNull(transport, "transport");
        this.transport.addMessageListener(this::signalWorkAvailable);
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        loopThread = new Thread(this::runLoop, "split-client-loop");
        loopThread.setDaemon(true);
        loopThread.start();
    }

    public void submitInput(String rawInput) {
        queuedInputs.add(Objects.requireNonNull(rawInput, "rawInput"));
        signalWorkAvailable();
    }

    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        signalWorkAvailable();
        if (loopThread == null) {
            return;
        }

        loopThread.interrupt();
        try {
            loopThread.join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void runLoop() {
        while (running.get()) {
            TickControl tickControl = client.getTickControl();
            boolean timedWake = awaitWake(tickControl);
            if (!running.get()) {
                return;
            }

            if (timedWake && tickControl.isAutomatic()) {
                client.advanceTick();
                transport.advanceTick();
            }

            drainInputs();
            executeQueuedCommands();
            client.processIncomingMessages();

            TickControl updatedTickControl = client.getTickControl();
            if (updatedTickControl.isManual()) {
                client.advanceTick();
            }

            client.simulateCurrentTick();
        }
    }

    private boolean awaitWake(TickControl tickControl) {
        synchronized (wakeMonitor) {
            long observedWakeVersion = wakeVersion;
            if (tickControl.isAutomatic()) {
                long remainingNanos = TimeUnit.MILLISECONDS.toNanos(tickControl.getTickIntervalMillis());
                while (running.get() && wakeVersion == observedWakeVersion) {
                    if (remainingNanos <= 0L) {
                        return true;
                    }

                    long beforeWait = System.nanoTime();
                    try {
                        TimeUnit.NANOSECONDS.timedWait(wakeMonitor, remainingNanos);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                    remainingNanos -= System.nanoTime() - beforeWait;
                }

                return wakeVersion == observedWakeVersion;
            }

            while (running.get() && wakeVersion == observedWakeVersion) {
                try {
                    wakeMonitor.wait();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            return false;
        }
    }

    private void signalWorkAvailable() {
        synchronized (wakeMonitor) {
            wakeVersion += 1;
            wakeMonitor.notifyAll();
        }
    }

    private void drainInputs() {
        String rawInput;
        while ((rawInput = queuedInputs.poll()) != null) {
            ClientTerminalCommandParser.Result parseResult = parser.parse(rawInput);
            if (!parseResult.hasCommand()) {
                continue;
            }

            ClientTerminalCommand command = parseResult.getCommand();
            ClientTerminalCommandValidator.Result validationResult = validator.validate(command, client);
            if (!validationResult.isValid()) {
                System.out.println(validationResult.getMessage());
                continue;
            }

            queuedCommands.add(command);
        }
    }

    private void executeQueuedCommands() {
        ClientTerminalCommand command;
        while ((command = queuedCommands.poll()) != null) {
            executor.execute(command, client);
        }
    }
}