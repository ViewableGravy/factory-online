package com.factoryonline.client;

import java.io.IOException;

import com.factoryonline.client.bootstrap.ClientApplication;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws IOException {
        ClientApplication.run(args);
    }
}