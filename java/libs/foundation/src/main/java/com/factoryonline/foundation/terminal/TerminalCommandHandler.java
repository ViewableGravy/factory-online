package com.factoryonline.foundation.terminal;

import static org.jline.builtins.Completers.TreeCompleter.node;

import java.io.Closeable;
import java.io.IOException;

import org.jline.builtins.Completers.TreeCompleter;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.reader.Widget;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import com.factoryonline.foundation.config.TerminalCommands;

public final class TerminalCommandHandler implements Closeable {
    private final Terminal terminal;
    private final LineReader lineReader;

    private TerminalCommandHandler(TreeCompleter completer, boolean bindArrowCommands) throws IOException {
        this.terminal = createTerminal();
        this.lineReader = LineReaderBuilder.builder()
            .terminal(terminal)
            .completer(completer)
            .build();

        if (bindArrowCommands) {
            bindArrowCommandWidgets();
        }
    }

    public static TerminalCommandHandler createClientHandler() throws IOException {
        return new TerminalCommandHandler(createClientCompleter(), true);
    }

    public static TerminalCommandHandler createServerHandler() throws IOException {
        return new TerminalCommandHandler(createServerCompleter(), false);
    }

    public String readCommand(String prompt) {
        try {
            return lineReader.readLine(prompt);
        } catch (UserInterruptException exception) {
            return TerminalCommands.EXIT_COMMAND;
        } catch (EndOfFileException exception) {
            return null;
        }
    }

    @Override
    public void close() throws IOException {
        terminal.close();
    }

    private void bindArrowCommandWidgets() {
        Widget incrementWidget = () -> submitCommand(TerminalCommands.INCREMENT_COMMAND);
        Widget decrementWidget = () -> submitCommand(TerminalCommands.DECREMENT_COMMAND);

        lineReader.getWidgets().put("submit-increment-command", incrementWidget);
        lineReader.getWidgets().put("submit-decrement-command", decrementWidget);

        KeyMap<Binding> mainKeyMap = lineReader.getKeyMaps().get(LineReader.MAIN);
        mainKeyMap.bind(incrementWidget, "\u001B[A");
        mainKeyMap.bind(decrementWidget, "\u001B[B");
    }

    private boolean submitCommand(String command) {
        lineReader.getBuffer().clear();
        lineReader.getBuffer().write(command);
        lineReader.callWidget(LineReader.ACCEPT_LINE);
        return true;
    }

    private static Terminal createTerminal() throws IOException {
        if (System.console() == null) {
            return TerminalBuilder.builder()
                .dumb(true)
                .build();
        }

        try {
            return TerminalBuilder.builder()
                .system(true)
                .provider("jni")
                .build();
        } catch (IOException | RuntimeException exception) {
            return TerminalBuilder.builder()
                .system(true)
                .build();
        }
    }

    private static TreeCompleter createClientCompleter() {
        return new TreeCompleter(
            node(TerminalCommands.SNAPSHOT_COMMAND),
            node(TerminalCommands.INCREMENT_COMMAND),
            node(TerminalCommands.DECREMENT_COMMAND),
            node(TerminalCommands.EXIT_COMMAND));
    }

    private static TreeCompleter createServerCompleter() {
        return new TreeCompleter(
            node(TerminalCommands.SNAPSHOT_COMMAND),
            node(TerminalCommands.ADD_SIMULATION_COMMAND),
            node(TerminalCommands.TICK_COMMAND),
            node(TerminalCommands.TICK_MODE_COMMAND),
            node(TerminalCommands.TICK_RATE_COMMAND),
            node(
                TerminalCommands.SERVER_COMMAND_PREFIX,
                node(TerminalCommands.INCREMENT_COMMAND),
                node(TerminalCommands.DECREMENT_COMMAND)),
            node(TerminalCommands.EXIT_COMMAND));
    }
}
