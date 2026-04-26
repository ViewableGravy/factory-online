# Java Workspace

This directory is the active implementation target for the project runtime.

The current proof-of-concept runs as split processes:

- `run-server.sh` starts a single server that owns the simulations and accepts client connections.
- `run-client.sh` starts a client that connects over TCP and subscribes to a random server simulation.

## Layout

```text
java/
  apps/
    client/
      src/main/java/com/factoryonline/client/
    server/
      src/main/java/com/factoryonline/server/Main.java
    tools/
      terminal-client/
      terminal-server/
  libs/
    foundation/
    simulation-core/
  build/
  Makefile
  run-client.sh
  run-server.sh
```

## Requirements

- JDK 11 or newer
- VS Code Extension Pack for Java (`vscjava.vscode-java-pack`)

## Run

From `java/`:

```bash
./run-server.sh
```

In another terminal:

```bash
./run-client.sh
```

Pass `-v` to show the underlying build and run commands before the application starts.

Or explicitly:

```bash
make run
```

and:

```bash
make run-client
```

## Build

```bash
make build
```

## Clean

```bash
make clean
```