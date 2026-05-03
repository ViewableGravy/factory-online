package com.factoryonline.transport.commands;

import java.util.Objects;

public final class AuthSuccessCommand extends ProtocolCommand {
    public final String token;

    public AuthSuccessCommand(String token) {
        this.token = Objects.requireNonNull(token, "token");
    }
}