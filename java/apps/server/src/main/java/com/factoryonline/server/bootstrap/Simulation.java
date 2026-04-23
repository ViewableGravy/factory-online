package com.factoryonline.server.bootstrap;

final class Simulation {
    public String name;

    Simulation(String name) {
        this.name = name;
    }

    void run(int updateTick) {
        try {
            Thread.sleep(5L);
            System.out.println("[name: " + name + "] ran on [tick: " + updateTick + "] in [thread: " + Thread.currentThread().getName() + "]");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}