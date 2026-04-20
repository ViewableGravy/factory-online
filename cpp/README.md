# C++ Workspace Structure

This directory is organized around a few explicit rules from the native rewrite notes:

- application entrypoints stay thin and compose reusable libraries,
- simulation is split into low-churn actor runtime and deterministic factory runtime,
- rendering consumes published projection data instead of live simulation state,
- networking is a first-class domain with separate realtime and deterministic simulation lanes,
- hot reload is supported through narrow plugin seams rather than by making core state reloadable.

## Layout

```text
cpp/
  apps/
    client/
    server/
    tools/
  libs/
    foundation/
      containers/
      diagnostics/
      ids/
      math/
      memory/
      threading/
      time/
    engine/
      assets/
      core/
      ecs/
      hotreload/
      platform/
      render/
        backend/
        frame/
        resources/
      scheduler/
      tasking/
    simulation/
      actors/
        replication/
        runtime/
        systems/
      authority/
        replication/
        scheduling/
        validation/
      factory/
        commands/
        io/
        partitions/
        systems/
        topology/
    projection/
      actors/
      debug/
      factory/
    networking/
      protocol/
      realtime/
      session/
      simulation/
      snapshots/
      transport/
    persistence/
      migrations/
      replay/
      saves/
      snapshots/
    plugins/
      debug/
      gameplay/
      render_extractors/
  tests/
    determinism/
    integration/
    networking/
    performance/
    unit/
  benchmarks/
  cmake/
  external/
```

## Why This Shape

### `apps/`

Keep executables thin.

- `client/` owns host orchestration, windowing glue, UI integration, startup, and binding together the render thread, simulation executor, and network manager.
- `server/` owns session hosting, authoritative scheduling, persistence wiring, and runtime composition for headless execution.
- `tools/` is for replay inspectors, snapshot utilities, asset processors, migration tools, and other one-off binaries that should not live inside the game executables.

### `libs/foundation/`

This is the low-level base layer for code that should remain reusable and dependency-light: time, ids, memory primitives, math, threading helpers, diagnostics, and containers.

Keep this layer gameplay-agnostic.

### `libs/engine/`

This is the host runtime surface, not the home for all game logic.

- `core/` is where the runtime instance, service registry, scene or world orchestration, and lifecycle ownership should live.
- `scheduler/` owns named phases, system registration, and deterministic update ordering.
- `tasking/` owns worker-pool and job execution infrastructure. It should stay separate from simulation rules so client and server can use different worker topologies without changing gameplay code.
- `render/` owns renderer internals only: device setup, frame management, and GPU resources.
- `ecs/` stays available for low-churn actor state and tooling-facing composition, but it is intentionally not the center of factory simulation.
- `hotreload/` is the host side of reloadable module support. Long-lived state remains in the host runtime.

### `libs/simulation/`

This is the shared gameplay truth.

- `actors/` is for ECS-like low-churn runtime state such as players, NPCs, world props, and systems that are cheap to replicate directly.
- `factory/` is the deterministic, partitioned simulation runtime. It owns commands, topology, partition-local storage, explicit IO boundaries, and high-churn systems.
- `authority/` is the server-authoritative wrapper around shared simulation. It validates intent, schedules authoritative ticks, and manages correction or replication policy without duplicating the underlying simulation model.

This split keeps the client and server sharing most simulation code while still giving the server a clear place to add authoritative control.

### `libs/projection/`

Projection is a first-class boundary because render must consume published read models instead of live simulation memory.

- `actors/` converts actor runtime state into render-friendly or UI-friendly extracts.
- `factory/` converts deterministic factory state into compact draw-oriented buffers.
- `debug/` holds overlays and inspection views that should read published state rather than poke live systems.

### `libs/networking/`

Networking is not buried inside the engine because it has its own protocol, transport, and sync semantics.

- `transport/` and `session/` handle connection lifecycle and delivery guarantees.
- `protocol/` defines the typed messages shared by client and server.
- `realtime/` is the low-churn replication lane.
- `simulation/` is the deterministic command lane with tick metadata, readiness, lag-window health, and resync control.
- `snapshots/` exists because hydrate, reconnect, late-join, and divergence recovery remain first-class needs even in a command-driven architecture.

### `libs/persistence/`

Persistence stays separate from live runtime mutation.

- `saves/` and `snapshots/` own versioned formats.
- `replay/` supports deterministic verification and debugging.
- `migrations/` isolates version upgrade logic instead of spreading it across runtime code.

### `libs/plugins/`

This is the reloadable module surface.

- `gameplay/` is where user systems and optional gameplay features can live behind a narrow plugin ABI.
- `render_extractors/` is for hot-swappable extraction or presentation modules that do not own authoritative state.
- `debug/` is for developer-only overlays and inspection modules.

The rule here is that plugins own behavior and registration, while the host engine owns memory, scheduling, networking state, and long-lived world data.

## Growth Rules

- Prefer adding focused libraries under these branches instead of creating a broad `shared/` bucket.
- When a module grows real code, give it a narrow public header surface and a private implementation surface rather than exposing everything from one folder.
- Keep deterministic simulation storage and rules out of render code.
- Keep cross-partition behavior explicit through `factory/io/` and `authority/` rather than hidden reads across partitions.
- Keep tests mirrored to the architecture: unit tests for focused libraries, determinism tests for replay and partition hashing, networking tests for the two-lane protocol, and integration tests for client or server composition.

## Expected Next Step

As real code lands, each leaf library should grow its own `include/`, `src/`, and `CMakeLists.txt` where that helps keep public interfaces narrow. The current structure is the domain map, not the final per-library file layout.

## Bootstrap Build

The repository now includes a minimal server bootstrap under `apps/server/src/main.cpp`.

- Preferred long-term build system: CMake.
- Minimal runnable command in the current environment: `make run` from `cpp/`.
- Convenience wrapper: `./run-server.sh` from `cpp/`. It uses CMake when available and falls back to `make run` otherwise.

This keeps the project on a standard CMake path without blocking the first runnable server on a missing local dependency.

## Basic Commands

Start the server from `cpp/`:

```bash
./run-server.sh
```

What happens when you run that command:

1. The script checks whether `cmake` exists on your machine.
2. If it does, it configures a debug build and builds the `factory_server` executable.
3. If it does not, it falls back to `make run`.
4. It launches the server executable in the same terminal.

Stop the server:

- Press `Ctrl+C` to terminate it immediately.
- Press `Ctrl+D` on an empty line to send end-of-input and let this simple console program exit naturally.

Clean generated files:

```bash
make clean
```

If you are using the CMake path later, the equivalent cleanup is:

```bash
rm -rf build
```

Once `cmake` is installed, a more explicit CMake workflow from `cpp/` looks like this:

```bash
cmake --preset debug
cmake --build --preset debug
./build/debug/apps/server/factory_server
```

That breaks the process into three steps:

1. `cmake --preset debug` configures the project and generates build files.
2. `cmake --build --preset debug` compiles the code.
3. Running the executable starts the server.

For now, `./run-server.sh` is the simplest entrypoint.

## What The CMake Files Do

### Top-level `CMakeLists.txt`

This is the root build script for the C++ project. CMake starts here.

```cmake
cmake_minimum_required(VERSION 3.25)
```

This says: do not try to build this project with an older CMake than 3.25. It protects you from subtle differences in older CMake versions.

```cmake
project(
  factory_online
  VERSION 0.1.0
  DESCRIPTION "Factory Online native server bootstrap"
  LANGUAGES CXX
)
```

This defines the project itself.

- `factory_online` is the project name.
- `VERSION 0.1.0` is metadata for the project version.
- `DESCRIPTION` is human-readable metadata.
- `LANGUAGES CXX` tells CMake this project is using C++.

```cmake
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
```

This configures the compiler rules.

- `CMAKE_CXX_STANDARD 20` means compile as C++20.
- `CMAKE_CXX_STANDARD_REQUIRED ON` means do not silently fall back to an older standard.
- `CMAKE_CXX_EXTENSIONS OFF` means prefer standard C++ instead of compiler-specific dialects like `gnu++20`.

```cmake
add_subdirectory(apps/server)
```

This tells CMake: go into `apps/server/` and keep reading build rules from there.

That is how larger C++ projects stay organized. The root build file stays short, and each module or app owns its own build rules.

### `apps/server/CMakeLists.txt`

This file defines the actual server executable.

```cmake
add_executable(factory_server)
```

This creates a build target named `factory_server`. A target is just something CMake knows how to build. In this case, it is an executable program.

```cmake
target_sources(
  factory_server
  PRIVATE
    src/main.cpp
)
```

This attaches source files to that executable.

- `factory_server` is the target being configured.
- `PRIVATE` means these source files are internal to this target.
- `src/main.cpp` is the source file to compile.

```cmake
target_compile_features(factory_server PRIVATE cxx_std_20)
```

This says the target requires C++20 features.

Right now this is slightly redundant because the top-level file already sets C++20 globally, but it is still useful as the project grows because it makes the target's requirement explicit.

### `CMakePresets.json`

This file is not source code. It is a saved set of CMake command options.

Without presets, you would have to remember long commands or repeat them manually. Presets give names to common build setups.

```json
{
  "version": 6,
  "cmakeMinimumRequired": {
    "major": 3,
    "minor": 25,
    "patch": 0
  }
}
```

This is metadata for the preset file format and the minimum CMake version expected to understand it.

```json
"configurePresets": [
  {
    "name": "debug",
    "displayName": "Debug",
    "generator": "Ninja",
    "binaryDir": "${sourceDir}/build/debug",
    "cacheVariables": {
      "CMAKE_BUILD_TYPE": "Debug"
    }
  }
]
```

This defines one configure preset named `debug`.

- `name: debug` means you can run `cmake --preset debug`.
- `generator: Ninja` means CMake will generate Ninja build files when Ninja is installed.
- `binaryDir` is where all generated build output goes. That is why the build ends up under `cpp/build/debug/`.
- `CMAKE_BUILD_TYPE=Debug` means compile a debug build, which is the normal starting point while developing.

```json
"buildPresets": [
  {
    "name": "debug",
    "configurePreset": "debug",
    "targets": [
      "factory_server"
    ]
  }
]
```

This defines a build preset, also named `debug`.

It means: after configuring with the `debug` preset, build the `factory_server` target.

That is why this command works:

```bash
cmake --build --preset debug
```

You are telling CMake to build using the saved `debug` instructions instead of typing every option again.
