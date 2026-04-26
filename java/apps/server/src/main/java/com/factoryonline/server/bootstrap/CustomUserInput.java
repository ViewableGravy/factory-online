package com.factoryonline.server.bootstrap;

import java.util.Objects;

import com.factoryonline.foundation.config.TerminalCommands;

public final class CustomUserInput {
    public enum Kind {
        INCREMENT,
        DECREMENT,
        EXIT,
        CONTINUE
    }

    private static final String ESCAPE = "\u001B";
    private static final String ARROW_UP = ESCAPE + "[A";
    private static final String ARROW_DOWN = ESCAPE + "[B";

    private final String raw;
    private final Kind kind;

    private CustomUserInput(String raw, Kind kind) {
        this.raw = Objects.requireNonNull(raw, "raw");
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    public static CustomUserInput fromRaw(String raw) {
        String validatedRaw = Objects.requireNonNull(raw, "raw");
        String normalizedRaw = validatedRaw.strip();

        if (validatedRaw.isEmpty()) {
            return new CustomUserInput(validatedRaw, Kind.CONTINUE);
        }

        if (ARROW_UP.equals(validatedRaw) || normalizedRaw.equalsIgnoreCase(TerminalCommands.INCREMENT_COMMAND)) {
            return new CustomUserInput(validatedRaw, Kind.INCREMENT);
        }

        if (ARROW_DOWN.equals(validatedRaw) || normalizedRaw.equalsIgnoreCase(TerminalCommands.DECREMENT_COMMAND)) {
            return new CustomUserInput(validatedRaw, Kind.DECREMENT);
        }

        if (ESCAPE.equals(validatedRaw)
            || normalizedRaw.equalsIgnoreCase(TerminalCommands.ESCAPE_COMMAND)
            || normalizedRaw.equalsIgnoreCase(TerminalCommands.EXIT_COMMAND)) {
            return new CustomUserInput(validatedRaw, Kind.EXIT);
        }

        return new CustomUserInput(validatedRaw, Kind.CONTINUE);
    }

    public String getRaw() {
        return raw;
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isIncrement() {
        return kind == Kind.INCREMENT;
    }

    public boolean isDecrement() {
        return kind == Kind.DECREMENT;
    }

    public boolean isExit() {
        return kind == Kind.EXIT;
    }

    public boolean isContinue() {
        return kind == Kind.CONTINUE;
    }
}