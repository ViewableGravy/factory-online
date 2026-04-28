package com.factoryonline.server.bootstrap;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.factoryonline.foundation.timing.TickControl;
import com.factoryonline.server.bootstrap.commands.ServerTerminalCommand;
import com.factoryonline.server.bootstrap.commands.ServerTerminalCommandExecutor;
import com.factoryonline.server.bootstrap.commands.ServerTerminalCommandParser;
import com.factoryonline.server.bootstrap.commands.ServerTerminalCommandValidator;
import com.factoryonline.transport.ServerTransport;

public final class ServerRuntimeLoop {
    private final ServerApplication server;
    private final Queue<String> queuedInputs = new ConcurrentLinkedQueue<>();
    private final Queue<ServerTerminalCommand> queuedCommands = new ConcurrentLinkedQueue<>();
    private final ServerTerminalCommandParser parser = new ServerTerminalCommandParser();
    private final ServerTerminalCommandValidator validator = new ServerTerminalCommandValidator();
    private final ServerTerminalCommandExecutor executor = new ServerTerminalCommandExecutor();
    private final Object wakeMonitor = new Object();
    private final AtomicBoolean running = new AtomicBoolean();
    private long wakeVersion;
    private Thread loopThread;

    public ServerRuntimeLoop(ServerApplication server, ServerTransport transport) {
        this.server = Objects.requireNonNull(server, "server");
        Objects.requireNonNull(transport, "transport").addMessageListener(this::signalWorkAvailable);
    }

    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        loopThread = new Thread(this::runLoop, "split-server-loop");
        loopThread.setDaemon(true);
        loopThread.start();
    }

    public void submitCommand(String rawCommand) {
        queuedInputs.add(Objects.requireNonNull(rawCommand, "rawCommand"));
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
            TickControl tickControl = server.getTickControl();
            boolean timedWake = awaitWake(tickControl);
            if (!running.get()) {
                return;
            }

            drainInputs();
            executeQueuedCommands();
            server.processIncomingMessages();

            TickControl updatedTickControl = server.getTickControl();
            if (updatedTickControl.isManual()) {
                int requestedTicks = server.drainRequestedManualTicks();
                for (int tickIndex = 0; tickIndex < requestedTicks; tickIndex += 1) {
                    server.advanceTick();
                    server.simulateCurrentTick();
                }
                continue;
            }

            if (!timedWake) {
                continue;
            }

            server.advanceTick();
            server.simulateCurrentTick();
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

    private void drainCommands() {
        ServerTerminalCommand command;
        while ((command = queuedCommands.poll()) != null) {
            executor.execute(command, server);
        }
    }

    private void drainInputs() {
        String rawCommand;
        while ((rawCommand = queuedInputs.poll()) != null) {
            ServerTerminalCommandParser.Result parseResult = parser.parse(rawCommand);
            if (!parseResult.hasCommand()) {
                if (parseResult.getMessage() != null) {
                    System.out.println(parseResult.getMessage());
                }
                continue;
            }

            ServerTerminalCommand command = parseResult.getCommand();
            ServerTerminalCommandValidator.Result validationResult = validator.validate(command, server);
            if (!validationResult.isValid()) {
                System.out.println(validationResult.getMessage());
                continue;
            }

            queuedCommands.add(command);
        }
    }

    private void executeQueuedCommands() {
        drainCommands();
    }
}