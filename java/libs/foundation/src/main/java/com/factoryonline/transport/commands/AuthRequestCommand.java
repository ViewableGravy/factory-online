package com.factoryonline.transport.commands;

import java.util.Objects;

public final class AuthRequestCommand extends ProtocolCommand {
    public final String username;
    public final String password;

    public AuthRequestCommand(String username, String password) {
        this.username = Objects.requireNonNull(username, "username");
        this.password = Objects.requireNonNull(password, "password");
    }
}