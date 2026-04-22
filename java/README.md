# Java Workspace

This directory is the active implementation target for the project runtime.

The current runnable application matches the existing bootstrap behavior in this repository:

- prints `Hello, world!`,
- prompts for console input,
- echoes each entered line until standard input closes.

## Layout

```text
java/
  apps/
    server/
      src/main/java/com/factoryonline/server/Main.java
  build/
  Makefile
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

Or explicitly:

```bash
make run
```

## Build

```bash
make build
```

## Clean

```bash
make clean
```