package com.factoryonline.foundation.config;

import java.util.concurrent.TimeUnit;

public final class RuntimeTiming {
    public static final int TICK_INTERVAL_MILLIS = 8;
    public static final long TICK_INTERVAL_NANOS = TimeUnit.MILLISECONDS.toNanos(TICK_INTERVAL_MILLIS);
    public static final int SERVER_TICK_SYNC_INTERVAL = 2;
    public static final int CLIENT_TARGET_LOCAL_BUFFER_TICKS = 4;
    public static final int CLIENT_LAG_TOLERANCE_TICKS = 2;
    public static final int CLIENT_HARD_CORRECTION_TICKS = 4;
    public static final int CLIENT_CATCH_UP_TICKS = 2;
    public static final int SERVER_INPUT_LEAD_TICKS = CLIENT_TARGET_LOCAL_BUFFER_TICKS * 2;
    public static final double CLIENT_RATE_ADJUSTMENT_GAIN = 0.20D;
}
