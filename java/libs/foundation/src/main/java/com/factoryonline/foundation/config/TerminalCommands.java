package com.factoryonline.foundation.config;

public final class TerminalCommands {
    public static final String SNAPSHOT_COMMAND = "/snapshot";
    public static final String ADD_SIMULATION_COMMAND = "/add-simulation";
    public static final String TICK_COMMAND = "/tick";
    public static final String TICK_MODE_COMMAND = "/tick-mode";
    public static final String TICK_RATE_COMMAND = "/tick-rate";
    public static final String SERVER_COMMAND_PREFIX = "/server";
    public static final String CLIENT_COMMAND_PREFIX = "/client";
    public static final String INCREMENT_COMMAND = "up";
    public static final String DECREMENT_COMMAND = "down";
    public static final String EXIT_COMMAND = "exit";
    public static final String ESCAPE_COMMAND = "esc";
    public static final String SERVER_DIRECTION_USAGE = SERVER_COMMAND_PREFIX + " "
        + INCREMENT_COMMAND + "|" + DECREMENT_COMMAND;
    public static final String TICK_USAGE = TICK_COMMAND + " [count]";
    public static final String TICK_MODE_USAGE = TICK_MODE_COMMAND + " manual|automatic";
    public static final String TICK_RATE_USAGE = TICK_RATE_COMMAND + " <millis>";
    public static final String CLIENT_SWITCH_USAGE = CLIENT_COMMAND_PREFIX + " X";
}