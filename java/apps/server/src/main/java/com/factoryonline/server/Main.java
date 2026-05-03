package com.factoryonline.server;

import java.io.IOException;

import com.factoryonline.foundation.config.NetworkConfig;
import com.factoryonline.foundation.config.RuntimeTiming;
import com.factoryonline.foundation.config.TerminalCommands;
import com.factoryonline.foundation.scheduler.LoopCadence;
import com.factoryonline.foundation.terminal.TerminalCommandHandler;
import com.factoryonline.foundation.timing.TickControl;
import com.factoryonline.server.bootstrap.BatchedSimulationRunner;
import com.factoryonline.server.bootstrap.Broadcaster;
import com.factoryonline.server.bootstrap.ServerApplication;
import com.factoryonline.server.bootstrap.ServerRuntimeLoop;
import com.factoryonline.server.bootstrap.ServerTickController;
import com.factoryonline.server.bootstrap.SimulationIdFactory;
import com.factoryonline.server.bootstrap.TerminalUiState;
import com.factoryonline.simulation.SimulationRegistry;
import com.factoryonline.simulation.tick.Scheduler;
import com.factoryonline.simulation.tick.Ticker;
import com.factoryonline.transport.tcp.TcpServerTransport;

public final class Main {
    public static void main(String[] args) throws IOException {
        LoopCadence.initialize();

        /***** INSTANTIATE *****/
        TcpServerTransport transport = new TcpServerTransport(NetworkConfig.DEFAULT_PORT);
        Ticker ticker = new Ticker();
        ServerTickController tickController = new ServerTickController(TickControl.automatic(RuntimeTiming.TICK_INTERVAL_MILLIS));
        SimulationRegistry registry = new SimulationRegistry();
        BatchedSimulationRunner runner = new BatchedSimulationRunner(2, "server");
        Broadcaster broadcaster = new Broadcaster(transport);
        SimulationIdFactory simulationIdFactory = new SimulationIdFactory();

        /***** INITIALIZE *****/
        ServerApplication server = ServerApplication.builder()
            .transport(transport)
            .ticker(ticker)
            .tickController(tickController)
            .registry(registry)
            .runner(runner)
            .broadcaster(broadcaster)
            .simulationIdFactory(simulationIdFactory)
            .build()
            .configureDefault();

        ServerRuntimeLoop loop = new ServerRuntimeLoop(server, tickController, transport);

        Scheduler.register(server::applyBufferedInputs);
        Scheduler.register(runner::runTick);
        Scheduler.register(server::broadcastCurrentTickStateIfDue);

        /***** START *****/
        loop.start();

        System.out.println(
            TerminalUiState.getInstance().formatServerLabel() + " listening on port " + NetworkConfig.DEFAULT_PORT);

        TerminalCommandHandler.awaitServerCommands(prompt(), loop::submitCommand);

        /***** CLEANUP *****/
        loop.stop();
        server.cleanup();
        transport.closeNoThrow();
    }

    private static String prompt() {
        return "Server ["
            + TerminalCommands.SNAPSHOT_COMMAND
            + ", "
            + TerminalCommands.ADD_SIMULATION_COMMAND
            + ", "
            + TerminalCommands.TICK_USAGE
            + ", "
            + TerminalCommands.TICK_MODE_USAGE
            + ", "
            + TerminalCommands.TICK_RATE_USAGE
            + ", "
            + TerminalCommands.SERVER_DIRECTION_USAGE
            + ", "
            + TerminalCommands.EXIT_COMMAND
            + "]: ";
    }
}
