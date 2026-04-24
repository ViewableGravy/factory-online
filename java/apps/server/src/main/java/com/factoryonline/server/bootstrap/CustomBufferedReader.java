package com.factoryonline.server.bootstrap;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

public final class CustomBufferedReader implements Closeable {
    private final BufferedReader reader;

    public CustomBufferedReader(Reader reader) {
        this.reader = new BufferedReader(Objects.requireNonNull(reader, "reader"));
    }

    public CustomUserInput readLine() throws IOException {
        String rawInput = reader.readLine();
        if (rawInput == null) {
            return null;
        }

        return CustomUserInput.fromRaw(rawInput);
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
}