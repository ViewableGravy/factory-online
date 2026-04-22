package com.factoryonline.simulation;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class NamedThreadFactory implements ThreadFactory {
    private final AtomicInteger threadNumber = new AtomicInteger(0);
    private final String threadNamePrefix;

    public NamedThreadFactory(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        return new Thread(runnable, threadNamePrefix + "-" + threadNumber.incrementAndGet());
    }
}