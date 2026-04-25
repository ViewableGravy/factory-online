package com.factoryonline.server.bootstrap;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class CustomBufferedReader implements Closeable {
    private static final Path TERMINAL_DEVICE = Path.of("/dev/tty");
    private static final int ESCAPE = 27;
    private static final int BACKSPACE = 8;
    private static final int DELETE = 127;

    private final InputStream inputStream;
    private final InputStream terminalInputStream;
    private final byte[] singleByteBuffer = new byte[1];
    private final StringBuilder pendingLine = new StringBuilder();
    private final boolean rawModeEnabled;
    private final String originalTtyState;

    private boolean skipNextLineFeed;

    public CustomBufferedReader(InputStream inputStream) throws IOException {
        this.inputStream = Objects.requireNonNull(inputStream, "inputStream");
        this.terminalInputStream = tryOpenTerminalInputStream();

        if (terminalInputStream == null) {
            this.rawModeEnabled = false;
            this.originalTtyState = null;
            return;
        }

        String ttyState = tryCaptureTtyState();
        if (ttyState == null) {
            this.rawModeEnabled = false;
            this.originalTtyState = null;
            return;
        }

        enableRawMode();
        this.rawModeEnabled = true;
        this.originalTtyState = ttyState;
    }

    public CustomUserInput readLine() throws IOException {
        if (!rawModeEnabled) {
            return readBufferedLine();
        }

        while (true) {
            int nextByte = readNextByte();
            if (nextByte == -1) {
                if (pendingLine.length() == 0) {
                    if (shouldKeepWaitingOnTerminal()) {
                        continue;
                    }

                    return null;
                }

                System.out.println();
                return consumePendingLine();
            }

            if (skipNextLineFeed && nextByte == '\n') {
                skipNextLineFeed = false;
                continue;
            }

            skipNextLineFeed = false;

            if (nextByte == '\r') {
                skipNextLineFeed = true;
                System.out.println();
                return consumePendingLine();
            }

            if (nextByte == '\n') {
                System.out.println();
                return consumePendingLine();
            }

            if (nextByte == BACKSPACE || nextByte == DELETE) {
                eraseLastCharacter();
                continue;
            }

            if (nextByte == ESCAPE) {
                return readEscapeInput();
            }

            pendingLine.append((char) nextByte);
            System.out.print((char) nextByte);
            System.out.flush();
        }
    }

    @Override
    public void close() throws IOException {
        IOException restoreFailure = null;
        if (rawModeEnabled) {
            try {
                restoreTtyState();
            } catch (IOException exception) {
                restoreFailure = exception;
            }
        }

        try {
            if (terminalInputStream != null) {
                terminalInputStream.close();
            } else {
                inputStream.close();
            }
        } catch (IOException exception) {
            if (restoreFailure != null) {
                exception.addSuppressed(restoreFailure);
            }
            throw exception;
        }

        if (restoreFailure != null) {
            throw restoreFailure;
        }
    }

    private CustomUserInput readBufferedLine() throws IOException {
        pendingLine.setLength(0);

        while (true) {
            int nextByte = getActiveInputStream().read();
            if (nextByte == -1) {
                if (pendingLine.length() == 0) {
                    if (shouldKeepWaitingOnTerminal()) {
                        continue;
                    }

                    return null;
                }

                return consumePendingLine();
            }

            if (nextByte == '\r') {
                return consumePendingLine();
            }

            if (nextByte == '\n') {
                return consumePendingLine();
            }

            pendingLine.append((char) nextByte);
        }
    }

    private CustomUserInput readEscapeInput() throws IOException {
        pendingLine.setLength(0);

        int bracketByte = readPendingByte();
        if (bracketByte != '[') {
            return CustomUserInput.fromRaw("\u001B");
        }

        int directionByte = readPendingByte();
        if (directionByte == 'A') {
            return CustomUserInput.fromRaw("\u001B[A");
        }

        if (directionByte == 'B') {
            return CustomUserInput.fromRaw("\u001B[B");
        }

        return CustomUserInput.fromRaw("\u001B");
    }

    private int readNextByte() throws IOException {
        while (true) {
            int bytesRead = getActiveInputStream().read(singleByteBuffer);
            if (bytesRead == -1) {
                return -1;
            }

            if (bytesRead > 0) {
                return Byte.toUnsignedInt(singleByteBuffer[0]);
            }
        }
    }

    private int readPendingByte() throws IOException {
        while (true) {
            int bytesRead = getActiveInputStream().read(singleByteBuffer);
            if (bytesRead == -1) {
                return -1;
            }

            if (bytesRead > 0) {
                return Byte.toUnsignedInt(singleByteBuffer[0]);
            }
        }
    }

    private CustomUserInput consumePendingLine() {
        String rawInput = pendingLine.toString();
        pendingLine.setLength(0);
        return CustomUserInput.fromRaw(rawInput);
    }

    private void eraseLastCharacter() {
        if (pendingLine.length() == 0) {
            return;
        }

        pendingLine.deleteCharAt(pendingLine.length() - 1);
        System.out.print("\b \b");
        System.out.flush();
    }

    private String tryCaptureTtyState() {
        if (terminalInputStream == null) {
            return null;
        }

        try {
            return runSttyCommand("stty", "-g");
        } catch (IOException exception) {
            return null;
        }
    }

    private void enableRawMode() throws IOException {
        // Non-canonical mode lets arrow keys reach the app immediately instead of waiting for Enter.
        runSttyCommand("stty", "-icanon", "min", "0", "time", "1", "-echo");
    }

    private void restoreTtyState() throws IOException {
        runSttyCommand("stty", originalTtyState);
    }

    private InputStream getActiveInputStream() {
        if (terminalInputStream != null) {
            return terminalInputStream;
        }

        return inputStream;
    }

    private boolean shouldKeepWaitingOnTerminal() {
        return terminalInputStream != null;
    }

    private InputStream tryOpenTerminalInputStream() {
        if (!Files.isReadable(TERMINAL_DEVICE)) {
            return null;
        }

        try {
            return Files.newInputStream(TERMINAL_DEVICE);
        } catch (IOException exception) {
            return null;
        }
    }

    private String runSttyCommand(String... command) throws IOException {
        Process process = new ProcessBuilder(command)
            .redirectInput(TERMINAL_DEVICE.toFile())
            .start();

        byte[] stdout = process.getInputStream().readAllBytes();
        byte[] stderr = process.getErrorStream().readAllBytes();

        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException(new String(stderr, StandardCharsets.UTF_8).strip());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while running stty", exception);
        }

        return new String(stdout, StandardCharsets.UTF_8).strip();
    }
}