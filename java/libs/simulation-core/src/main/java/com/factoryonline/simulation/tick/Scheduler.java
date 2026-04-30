package com.factoryonline.simulation.tick;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Scheduler {
    private static final List<TickHandler> HANDLERS = new ArrayList<>();

    private Scheduler() {
    }

    public static void register(TickHandler handler) {
        synchronized (HANDLERS) {
            HANDLERS.add(Objects.requireNonNull(handler, "handler"));
        }
    }

    static void run(long tick) {
        List<TickHandler> currentHandlers;
        synchronized (HANDLERS) {
            currentHandlers = List.copyOf(HANDLERS);
        }

        for (TickHandler handler : currentHandlers) {
            handler.run(tick);
        }
    }

    static void clearForTesting() {
        synchronized (HANDLERS) {
            HANDLERS.clear();
        }
    }
}
