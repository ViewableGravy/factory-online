package com.factoryonline.client;

import java.io.IOException;
import java.util.concurrent.locks.LockSupport;

import com.factoryonline.client.bootstrap.App;
import com.factoryonline.client.bootstrap.ClientApplication;
import com.factoryonline.client.bootstrap.ClientRuntimeLoop;
import com.factoryonline.foundation.config.NetworkConfig;
import com.factoryonline.foundation.config.TerminalCommands;
import com.factoryonline.foundation.scheduler.LoopCadence;
import com.factoryonline.foundation.terminal.TerminalCommandHandler;
import com.factoryonline.server.bootstrap.TerminalUiState;
import com.factoryonline.simulation.tick.Scheduler;
import com.factoryonline.transport.commands.AuthRequestCommand;
import com.factoryonline.transport.commands.AuthSuccessCommand;
import com.factoryonline.transport.commands.RejectionCommand;
import com.factoryonline.transport.tcp.TcpClientTransport;

public final class Main {
    public static void main(String[] args) throws IOException {
        LoopCadence.initialize();

        /***** INSTANTIATE *****/
        TcpClientTransport transport = new TcpClientTransport(
            NetworkConfig.DEFAULT_HOST,
            NetworkConfig.DEFAULT_PORT
        );

        ClientApplication app = App.singleton();
        ClientRuntimeLoop runtimeLoop = new ClientRuntimeLoop(app, transport);

        /***** INITIALIZE *****/
        Scheduler.register(app::applyQueuedActions);
        Scheduler.register(app::runAttachedSimulations);
        Scheduler.register(app::compareQueuedChecksum);

        transport.initialize(App.clientId);
        app.initialize(transport);

        /***** START *****/
        transport.start();

        authenticate(transport);

        runtimeLoop.start();

        printConnectedMessage();

        TerminalCommandHandler.awaitClientCommands(prompt(), runtimeLoop::submitInput);
        
        /***** CLEANUP *****/
        runtimeLoop.stop();
        app.cleanup();
    }

    private static void authenticate(TcpClientTransport transport) throws IOException {
        String username;
        String password;

        try (TerminalCommandHandler loginHandler = TerminalCommandHandler.createClientHandler()) {
            username = loginHandler.readCommand("Username: ");
            password = loginHandler.readCommand("Password: ");
        }

        transport.send(new AuthRequestCommand(username, password), false);

        while (true) {
            for (AuthSuccessCommand authSuccess : transport.drainAs(AuthSuccessCommand.class)) {
                App.sessionToken = authSuccess.token;
                App.singleton().onAuthenticated();
                return;
            }

            for (RejectionCommand rejection : transport.drainAs(RejectionCommand.class)) {
                System.out.println(
                    "Client " + TerminalUiState.getInstance().formatClient(App.singleton().clientId)
                        + " received Rej for "
                        + TerminalUiState.getInstance().formatSimulation(rejection.simulationId)
                        + " at tick " + rejection.tick + ": " + rejection.message);
            }

            LockSupport.parkNanos(100_000_000L);
        }
    }

    private static String prompt() {
        return "Client ["
            + TerminalUiState.getInstance().formatClient(App.singleton().clientId)
            + "] "
            + TerminalCommands.SNAPSHOT_COMMAND
            + ", "
            + TerminalCommands.INCREMENT_COMMAND
            + "/"
            + TerminalCommands.DECREMENT_COMMAND
            + "=apply, "
            + TerminalCommands.EXIT_COMMAND
            + "=quit: ";
    }

    private static void printConnectedMessage() {
        System.out.println(
            "Client " + TerminalUiState.getInstance().formatClient(App.singleton().clientId) + " connected");
    }
}
