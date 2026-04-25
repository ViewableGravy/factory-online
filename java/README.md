# Java Workspace

This directory is the active implementation target for the project runtime.

The current runnable application matches the existing bootstrap behavior in this repository:

- prints `Hello, world!`,
- prompts for console input,
- echoes each entered line until standard input closes.
- keeps networking as documented requirements only for now.

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
  run-server.sh
```

Networking is intentionally not implemented in code yet. The requirements and future layout are documented in the vault note referenced from [documentation/networking.md](/home/gravy/programming/games/factory-online/documentation/networking.md).

## Requirements

- JDK 11 or newer
- VS Code Extension Pack for Java (`vscjava.vscode-java-pack`)

## Run

From `java/`:

```bash
./run-server.sh
```

Pass `-v` to show the underlying build and run commands before the application starts.

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