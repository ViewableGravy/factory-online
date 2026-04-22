package com.factoryonline.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Hello, world!");
        System.out.print("Enter text and press Enter: ");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String userInput;
        while ((userInput = reader.readLine()) != null) {
            System.out.println("You entered: " + userInput);
            System.out.print("Enter text and press Enter: ");
        }
    }
}