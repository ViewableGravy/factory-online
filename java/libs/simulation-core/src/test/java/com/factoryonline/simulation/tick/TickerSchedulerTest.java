package com.factoryonline.simulation.tick;

import java.util.ArrayList;
import java.util.List;

public final class TickerSchedulerTest {
    private TickerSchedulerTest() {
    }

    public static void main(String[] args) {
        runsHandlersInRegistrationOrder();
        queuesTicksTriggeredDuringExecution();
        preventsReentrantTickExecution();
        queuesInitializationAfterActiveTick();
    }

    private static void runsHandlersInRegistrationOrder() {
        Scheduler.clearForTesting();
        Ticker ticker = new Ticker();
        List<String> events = new ArrayList<>();

        Scheduler.register(tick -> events.add("first:" + tick));
        Scheduler.register(tick -> events.add("second:" + tick));

        ticker.tick();

        assertEquals(List.of("first:1", "second:1"), events, "handlers must run in registration order");
        assertEquals(1L, ticker.getCurrentTick(), "ticker should advance once");
    }

    private static void queuesTicksTriggeredDuringExecution() {
        Scheduler.clearForTesting();
        Ticker ticker = new Ticker();
        List<String> events = new ArrayList<>();

        Scheduler.register(tick -> {
            events.add("first:" + tick);

            if (tick == 1L) {
                ticker.tick();
                ticker.tick();
            }
        });
        Scheduler.register(tick -> events.add("second:" + tick));

        ticker.tick();

        assertEquals(
            List.of("first:1", "second:1", "first:2", "second:2", "first:3", "second:3"),
            events,
            "ticks triggered during execution must drain after the active tick");
        assertEquals(3L, ticker.getCurrentTick(), "queued ticks should all run sequentially");
    }

    private static void preventsReentrantTickExecution() {
        Scheduler.clearForTesting();
        Ticker ticker = new Ticker();
        boolean[] tickIsRunning = { false };
        int[] maxConcurrentTicks = { 0 };
        int[] runningTicks = { 0 };

        Scheduler.register(tick -> {
            if (tickIsRunning[0]) {
                throw new AssertionError("tick execution reentered while another tick was running");
            }

            tickIsRunning[0] = true;
            runningTicks[0]++;
            maxConcurrentTicks[0] = Math.max(maxConcurrentTicks[0], runningTicks[0]);

            if (tick == 1L) {
                ticker.tick();
            }

            runningTicks[0]--;
            tickIsRunning[0] = false;
        });

        ticker.tick();

        assertEquals(1, maxConcurrentTicks[0], "only one tick may execute at a time");
        assertEquals(2L, ticker.getCurrentTick(), "reentrant tick calls should be queued");
        Scheduler.clearForTesting();
    }

    private static void queuesInitializationAfterActiveTick() {
        Scheduler.clearForTesting();
        Ticker ticker = new Ticker();
        List<String> events = new ArrayList<>();

        Scheduler.register(tick -> {
            events.add("first:" + tick);

            if (tick == 1L) {
                ticker.queueInitialize(8L);
                ticker.tick();
            }
        });
        Scheduler.register(tick -> events.add("second:" + tick));

        ticker.tick();

        assertEquals(
            List.of("first:1", "second:1", "first:9", "second:9"),
            events,
            "queued initialization must run after the active tick and before later ticks");
        assertEquals(9L, ticker.getCurrentTick(), "ticker should advance from the initialized tick");
        Scheduler.clearForTesting();
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
