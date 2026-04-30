package com.factoryonline.server.bootstrap;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.factoryonline.foundation.scheduler.LoopCadence;
import com.factoryonline.foundation.timing.TickControl;
import com.factoryonline.server.bootstrap.commands.ServerTerminalCommand;
import com.factoryonline.server.bootstrap.commands.ServerTerminalCommandExecutor;
import com.factoryonline.server.bootstrap.commands.ServerTerminalCommandParser;
import com.factoryonline.server.bootstrap.commands.ServerTerminalCommandValidator;
import com.factoryonline.transport.ServerTransport;

public final class ServerRuntimeLoop {
    private final ServerApplication server;
    private final ServerTickController tickController;
    private final Queue<String> queuedInputs = new ConcurrentLinkedQueue<>();
    private final Queue<ServerTerminalCommand> queuedCommands = new ConcurrentLinkedQueue<>();
    private final Object wakeMonitor = new Object();
    private final AtomicBoolean running = new AtomicBoolean();
    private long wakeVersion;
    private Thread loopThread;

    public ServerRuntimeLoop(ServerApplication server, ServerTickController tickController, ServerTransport transport) {
        this.server = Objects.requireNonNull(server, "server");
        this.tickController = Objects.requireNonNull(tickController, "tickController");
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
            TickControl tickControl = tickController.getTickControl();
            boolean automaticTickDue = LoopCadence.beginCycle(tickControl);

            drainInputs();
            executeQueuedCommands();
            server.processIncomingMessages();

            TickControl updatedTickControl = tickController.getTickControl();
            if (updatedTickControl.isManual()) {
                int requestedTicks = tickController.drainRequestedManualTicks();
                for (int tickIndex = 0; tickIndex < requestedTicks; tickIndex += 1) {
                    server.advanceTick();
                }
            } else if (automaticTickDue) {
                server.advanceTick();
            }

            awaitEnd(updatedTickControl);
        }
    }

    private void awaitEnd(TickControl tickControl) {
        synchronized (wakeMonitor) {
            long observedWakeVersion = wakeVersion;
            if (tickControl.isAutomatic()) {
                while (running.get() && wakeVersion == observedWakeVersion) {
                    long remainingNanos = LoopCadence.remainingNanos(tickControl);
                    if (remainingNanos <= 0L) {
                        return;
                    }

                    try {
                        TimeUnit.NANOSECONDS.timedWait(wakeMonitor, remainingNanos);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                return;
            }

            while (running.get() && wakeVersion == observedWakeVersion) {
                try {
                    wakeMonitor.wait();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
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
            ServerTerminalCommandExecutor.execute(command, server, tickController);
        }
    }

    private void drainInputs() {
        String rawCommand;
        while ((rawCommand = queuedInputs.poll()) != null) {
            ServerTerminalCommandParser.Result parseResult = ServerTerminalCommandParser.parse(rawCommand);
            if (!parseResult.hasCommand()) {
                if (parseResult.message != null) {
                    System.out.println(parseResult.message);
                }
                continue;
            }

            ServerTerminalCommand command = parseResult.command;
            ServerTerminalCommandValidator.Result validationResult = ServerTerminalCommandValidator.validate(command, server, tickController);
            if (!validationResult.valid) {
                System.out.println(validationResult.message);
                continue;
            }

            queuedCommands.add(command);
        }
    }

    private void executeQueuedCommands() {
        drainCommands();
    }
}
