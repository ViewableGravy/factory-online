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
