# 19 - Native Rewrite Strategy for MMO Sandbox Factory Builder

## Executive Recommendation

Rewrite the game in C++, but do not rebuild the current TypeScript engine one-to-one.

The next codebase should be a game-specific native runtime with two first-class execution models:

1. An actor/runtime lane for low-churn world state such as players, NPCs, trees, camera, UI-facing entities, and other state that benefits from ECS-like composition and selective replication.
2. A deterministic factory/runtime lane for claim-local automation such as belts, carried items, machines, production graphs, and other high-churn simulation that should not be modeled or replicated as generic ECS diff traffic.

The most important architectural conclusion from this POC is that raw ECS diff replication is not the right long-term gameplay protocol for a Factorio-like MMO simulation. The current docs already point toward a hybrid model: typed commands as the gameplay protocol, snapshots for hydrate/resync, selective replication for low-churn actors, and partition-scoped deterministic simulation for factory systems.

This rewrite should keep the ideas that made this engine productive, but move authoritative simulation, networking, and memory behavior into a native runtime designed around your actual game shape rather than a general-purpose ECS-first engine.

Relevant evidence from this repository:

- [17-DETERMINISTIC-PARTITIONED-SIMULATION-ROADMAP.md](./17-DETERMINISTIC-PARTITIONED-SIMULATION-ROADMAP.md)
- [18-HYBRID-COMMAND-NETWORKING-PROTOCOL.md](./18-HYBRID-COMMAND-NETWORKING-PROTOCOL.md)
- [conveyor-system-design.md](../conveyor-system-design.md)
- [RENDERING_50K_120FPS_ROADMAP.md](../RENDERING_50K_120FPS_ROADMAP.md)
- [RENDERING_1M_ENTITY_AUDIT.md](../RENDERING_1M_ENTITY_AUDIT.md)

## Integration of the Pre-Investigation Notes

This document also incorporates a separate pre-investigation discussion that explored the same game shape without awareness of this codebase. That discussion was useful, but it was not codebase-aware, so the right approach is to merge the aligned ideas and explicitly reject the parts that conflict with the repo-backed direction.

### Strong alignment

- Simulation, projection, and rendering should be treated as separate responsibilities.
- Region-based isolation maps directly to the claim-local or partition-local simulation model already described in this repo.
- Trucks, trains, and bulk transfer systems should behave as explicit cross-partition IO boundaries rather than continuous shared simulation graphs.
- Desync should be treated as expected and recoverable, not exceptional.
- Clients need degradation strategies instead of assuming every client can always mirror every visible partition indefinitely.

### Needs adaptation

- "Events are cheaper than state" is directionally correct, but the repo-backed architecture is not event-only. The real shape is typed commands plus selective low-churn replication plus snapshots and resync support.
- A client-side ECS can still exist, but as a projection or presentation layer rather than the authoritative factory simulation substrate.
- Mirror mode versus observer mode is useful language, but it should be modeled as a client sync policy layered above the shared networking foundation rather than as a separate protocol.

### Do not import literally

- Immediate optimistic execution plus rollback or replay should not be the default contract for deterministic partition simulation. The repo direction is authoritative scheduling at future ticks with lag-windowed mirrored clients.
- "Full snapshots are rare fallback mechanisms" is only partly true. Snapshots must remain first-class for bootstrap, late join, reconnect, and resync even if they are not the steady-state hot path.
- The system should not depend on a general ECS bridge for all simulation output. The bridge should be a client-facing projection layer that can feed rendering, UI, debug views, or a small presentation ECS when helpful.

## What This POC Already Proved

### Keep the architectural lessons

- The current networking direction is already correct for this game: one shared command foundation, low-churn replication for actors, deterministic partition simulation for factory systems, and snapshots for bootstrap/recovery.
- The engine's update-vs-render split is correct and should survive the rewrite. Authoritative state changes belong in update; render should stay observational.
- `createSystem` is one of the strongest ideas in the repo. Named systems, explicit phases, initialize/cleanup hooks, and stateful system instances are worth preserving as a native API.
- Engine-scoped asset ownership is a good model. The current asset manager keeps asset lifetime attached to the runtime instance instead of relying on global singletons.
- Pooling and lifecycle-aware cleanup are valuable and should become more important in a native rewrite, not less.
- The repo already identified that conveyor simulation requires deterministic ordering and O(1) spatial access. That is a sign that the factory runtime should become a domain-specific simulation core rather than a generic ECS workload.

### Do not keep the accidental constraints

- Do not carry forward the assumption that every important system should be an ECS component/system pair.
- Do not keep raw ECS diffs as the universal network protocol.
- Do not keep one-entity-per-item rendering for high-cardinality conveyor traffic.
- Do not keep JavaScript as the authoritative simulation runtime for long-term MMO-scale factory logic.

## Language Recommendation

### Recommended: C++

C++ is the strongest fit for the rewrite if the priority is mature engine tooling, broad documentation, direct control over threading and memory layout, and a lower barrier to getting productive quickly in a native runtime.

Why C++ fits this project:

- It removes the JIT and garbage collector from the authoritative simulation path.
- It has the deepest body of existing game-engine and high-performance simulation documentation.
- It gives you first-class access to mature threading primitives, shared-library workflows, profiler support, custom allocators, and graphics tooling.
- It maps naturally onto the runtime split this game needs: client app, server app, protocol layer, actor runtime, deterministic simulation runtime, render backend, tools, and hot-reloadable gameplay modules.
- It is a strong fit for headless server simulation, deterministic replay harnesses, networking, binary serialization, and custom rendering backends.
- It makes DLL or shared-library based gameplay hot reload a straightforward first-class design choice rather than an awkward add-on.

### Why this is still a discipline problem, not just a language choice

C++ does make some early engine tasks easier to approach pragmatically, especially if you want to move quickly with DLL workflows and familiar low-level patterns. It also makes it easier to create lifetime bugs, data races, and ownership leaks if the architecture is sloppy.

That means the rewrite should accept C++ deliberately, not casually:

- strict single-writer ownership inside simulation partitions,
- explicit thread boundaries,
- narrow shared-library interfaces,
- strong sanitizer and profiler usage from day one,
- minimal hidden allocation and mutation,
- no tolerance for ad hoc cross-thread access.

If that discipline is present, C++ is a very reasonable choice for this project.

### Why not Rust as the first choice

Rust is still a strong option technically, but if the immediate priority is building momentum in a native engine with mature multithreading, DLL tooling, and an ecosystem you can move through faster today, C++ is the more practical choice.

The trade is straightforward:

- C++ gives you faster leverage and broader engine precedent.
- Rust gives you stronger compile-time safety and ownership enforcement.

For this rewrite, the architecture is already doing most of the heavy lifting. If you are willing to compensate for weaker safety with strong code review, sanitizers, and ownership rules, C++ is an acceptable and likely productive primary bet.

### Why not Zig as the first choice

Zig is interesting, but it is still the wrong primary bet for a project of this scope today. The language is promising, yet the ecosystem, tooling maturity, and battle-tested game/runtime infrastructure are still weaker than C++ for a long-running MMO factory project.

### Important reality check on determinism

Native code does not magically make simulation deterministic.

C++ solves the JIT problem, but you still need a real determinism contract:

- fixed-step simulation only,
- stable execution ordering,
- deterministic random sources,
- integer or fixed-point authoritative math for core simulation,
- explicit versioning for content and protocol,
- no hidden async mutation of gameplay state,
- no reliance on hash-map iteration order,
- no gameplay dependence on renderer timing.

If you use free-running floating-point simulation across machines and expect bit-perfect replay forever, C++ will not save you from that mistake.

## What To Keep

The rewrite should preserve these concepts, not necessarily these implementations.

### 1. System ergonomics

Preserve the spirit of [src/engine/src/core/system/index.ts](../../src/engine/src/core/system/index.ts):

- named systems,
- explicit update/render phases,
- initialize/cleanup lifecycle,
- opt-in enabled state,
- stable scheduling,
- room for hot swapping behavior while preserving system-owned state.

In the new codebase, this should become a small native scheduler API rather than a TypeScript ECS helper.

### 2. Update/render separation

Preserve the rule documented in the current repo: update owns authoritative state, render is read-only.

That rule matters even more in the rewrite because you want one renderer consuming state from two different runtimes.

### 3. Engine-scoped asset ownership

Preserve the design intent of [ASSET_MANAGEMENT.md](../ASSET_MANAGEMENT.md) and [src/engine/src/asset/AssetManager.ts](../../src/engine/src/asset/AssetManager.ts):

- assets owned by a runtime instance,
- typed handles or keys,
- explicit loading states,
- adapter-based loading pipeline,
- clean lifetime boundaries.

The native version should use typed asset handles and staged GPU upload, but the ownership model is worth keeping.

### 4. Pooling and frame-lifecycle memory patterns

Preserve the intent of [src/utils/src/pool.ts](../../src/utils/src/pool.ts):

- reuse transient allocations,
- make lifecycle boundaries explicit,
- tie cleanup to frame/update phases where possible.

In C++, this becomes slabs, arenas, frame allocators, scratch allocators, object pools, and reusable command buffers rather than a generic JavaScript object pool.

### 5. Type-first APIs

Preserve the general development style that this repo already demonstrates well:

- protocol types are explicit,
- system boundaries are named and typed,
- invalid states should be pushed to the boundary,
- optional features should be composable rather than globally entangled.

### 6. Plugin-style optionality

Preserve the architectural instinct shown in [08-PLUGIN-BASED-IMPLEMENTATION.md](./08-PLUGIN-BASED-IMPLEMENTATION.md): complex features should be built as modules on top of core primitives rather than baked into the lowest layer by default.

## What To Ditch

### 1. Generic ECS as the center of all gameplay

Do not build the new engine around the assumption that every simulation concern should be expressed as components plus generic queries.

That is still fine for actor state, scene state, and some visual data, but it is the wrong center of gravity for claim-local factory simulation.

### 2. ECS diff replication as the main gameplay protocol

Keep snapshots and diffs for these jobs:

- hydrate,
- reconnect,
- resync,
- debug inspection,
- persistence helpers.

Do not keep them as the main transport for factory gameplay.

### 3. High-cardinality simulation as render entities

Do not represent every carried belt item as a general-purpose replicated ECS entity if the runtime needs to support large-scale throughput.

The deterministic simulation should own compact item state. The renderer should consume extracted render buffers, instance streams, or other compact read models.

### 4. Implicit ordering assumptions

The conveyor design doc already identified that simulation correctness depends on deterministic ordering and O(1) spatial lookups. The rewrite should not depend on insertion-order side effects or general query iteration history for simulation correctness.

### 5. One-runtime-fits-all thinking

Do not force player movement, trees, belts, factories, and network replication into a single storage model just because it is convenient to conceptualize.

This game already wants at least two runtime models.

## What To Rebuild From Scratch

### 1. Deterministic partition simulation runtime

This is the core new investment.

Each land claim or tightly-coupled claim cluster should be a simulation partition with:

- partition id,
- authoritative tick,
- fixed-step execution,
- compact SoA-style data structures,
- explicit IO boundaries,
- periodic hash generation,
- snapshot/hydration support,
- clear ownership and visibility rules.

This runtime should be domain-specific. Belts, inserters, machine recipes, queues, lane buffers, power, fluids, and factory adjacency should be modeled as data structures that match the workload, not as generic actor entities.

### 2. Hybrid networking foundation

Rebuild networking around two first-class lanes.

#### Realtime replication lane

For low-churn actors and view-critical state:

- players,
- NPCs,
- camera-relevant transforms,
- ownership metadata,
- presence,
- coarse gameplay state that is cheap to correct.

#### Deterministic simulation lane

For partition-local factory simulation:

- typed intent commands,
- server-assigned execution tick,
- partition-local scheduling,
- progress reporting,
- hash checks,
- resync flow,
- snapshot bootstrap.

This is the architecture the current repo already converged on in [18-HYBRID-COMMAND-NETWORKING-PROTOCOL.md](./18-HYBRID-COMMAND-NETWORKING-PROTOCOL.md).

### 3. Renderer extraction pipeline

The renderer should consume two different read models:

1. Actor/scene extracts from the actor runtime.
2. Factory extracts from the deterministic simulation runtime.

Do not make the renderer care which runtime originally owned the data.

The renderer should receive compact draw-oriented data such as:

- instance transforms,
- sprite or material handles,
- animation state,
- belt/item lane visualization buffers,
- chunk visibility masks,
- debug overlays.

### 4. Deterministic test harness

Build this before the engine becomes large.

You need automated proof that:

- the same command stream produces the same partition hash,
- replay survives save/load,
- cross-partition IO obeys ordering rules,
- server and client mirrored execution stay aligned,
- simulation stays deterministic across debug and release builds.

### 5. Explicit save/hydrate/resync pipeline

Do not let this emerge accidentally from gameplay state mutation.

Build versioned snapshot formats and replay baselines intentionally. The networking design should assume late join, resync, and catch-up from day one.

## Recommended Runtime Architecture

The rewrite should have one host runtime, two simulation models, and a client-side projection layer.

```text
Native Host Runtime
  |- Asset services
  |- Pooling / scratch allocators
  |- Scheduler / module registry
  |- Network manager
  |
  |- Actor runtime (ECS-ish, low-churn, selectively replicated)
  |    |- players
  |    |- NPCs
  |    |- world props
  |    |- camera / input adapters
  |
  |- Factory runtime (deterministic, partitioned, domain-specific)
       |- claims
       |- belts
       |- items as compact data
       |- machines / recipes
       |- partition IO queues
  |
  |- Projection / extract layer
  |    |- actor projection
  |    |- factory projection
  |    |- correction smoothing
  |    |- render or ECS read models
  |
  |- Renderer
```

## Multithreading From Day One

Threading should not be treated as a later optimization pass. It needs to be part of the runtime contract from the start.

The core rule is:

- simulation does not share mutable state with rendering,
- rendering consumes extracted snapshots rather than live simulation memory,
- each deterministic partition has a single writer at any moment,
- cross-partition interaction happens through explicit queues applied at well-defined tick boundaries.

This is the only sane way to scale the game without rebuilding the core ownership model later.

### Thread boundaries we should design around immediately

From day one, assume these responsibilities are isolated:

- platform or host thread,
- render thread,
- network IO thread or runtime,
- deterministic simulation executor,
- background persistence or asset streaming workers.

The important point is not the exact OS-thread count on day one. The important point is that the APIs, data ownership, and transport boundaries are designed so these responsibilities can remain isolated as the project scales.

### Server threading model

On the server, simulation boundaries should be parallelizable from the start, but that does not mean "one permanent OS thread per land claim".

The better model is:

- one logical execution lane per active simulation partition,
- a fixed-size simulation worker pool,
- stable partition affinity so a hot partition tends to stay on the same worker when possible,
- explicit cross-partition mailboxes,
- no direct shared mutable reads across partition boundaries.

Recommended server topology:

```text
Server Host
  |- network/session runtime
  |- simulation scheduler
  |- worker pool
  |    |- worker 0: partition lanes
  |    |- worker 1: partition lanes
  |    |- worker 2: partition lanes
  |    |- worker N: partition lanes
  |- replication/projection jobs
  |- persistence/background jobs
```

Each partition is conceptually its own single-threaded deterministic world. The scheduler maps those worlds onto a smaller number of workers.

That gives you:

- parallelism across claims,
- deterministic single-writer execution within a claim,
- fewer idle threads,
- lower context-switch overhead,
- the ability to pin especially hot partitions to dedicated workers later if needed.

What we should not do by default:

- one OS thread per claim regardless of load,
- arbitrary work-stealing inside one partition step,
- cross-thread reads into neighboring partition state during simulation.

### Client threading model

On the client, the architecture should also be multi-threaded from day one, but the thread roles differ.

Recommended client topology:

```text
Client Host
  |- platform/main thread
  |    |- window/event pump
  |    |- input collection
  |    |- UI orchestration
  |    |- network bridge / runtime coordination
  |
  |- render thread
  |    |- owns graphics device
  |    |- consumes latest projection snapshot
  |
  |- simulation executor
  |    |- mirrored partition lanes
  |    |- catch-up / resync state
  |    |- command replay
  |
  |- asset streaming / background IO
```

If a target platform forces rendering to stay on the main thread, keep the same ownership model and move orchestration away from it. The architecture should still behave as if rendering is an isolated service that only consumes published snapshots.

### Render must stay separate from update and simulation

The render-thread document in this repo already points in the right direction: render should consume the newest extracted frame and never block simulation trying to present every intermediate state.

Carry that rule into the rewrite:

- simulation publishes render-friendly projection snapshots,
- render uses a latest-wins model,
- render backlog is not allowed to grow unbounded,
- render can be eventually consistent,
- gameplay truth stays in simulation, not in render memory.

In native terms, that usually means:

- double or triple buffered render snapshots,
- stable typed-array or SoA-style frame buffers,
- lock-free or minimally locked publish and consume edges,
- no renderer access to authoritative mutable partition state.

### Should every client partition get its own thread?

Not by default.

This is the most important threading decision in the document.

The server and client do not need identical physical thread layouts to remain deterministic. They need identical simulation contracts.

What must match between server and client:

- fixed-step rules,
- partition-local single-writer execution,
- command ordering,
- authoritative scheduled ticks,
- boundary queue semantics,
- deterministic hashing and resync behavior.

What does not need to match:

- one OS thread per partition,
- identical worker counts,
- identical core topology,
- identical catch-up pacing implementation.

The right abstraction is a logical partition lane, not a mandatory dedicated thread.

Each mirrored partition should behave as if it owns its own sequential deterministic lane. The runtime can then choose whether that lane is:

- multiplexed with other partitions on one worker,
- pinned to a dedicated worker,
- temporarily not mirrored at all and instead observed through authoritative projection data.

This keeps the simulation contract consistent while allowing the client to degrade gracefully.

### Why not force thread-per-boundary on the client?

Because it creates the wrong costs too early:

- too many workers or threads for modest hardware,
- idle overhead for partitions that are visible but not hot,
- more synchronization points than necessary,
- more memory duplication,
- harder scheduling when a client is mirroring many nearby claims.

The client needs to match server logic, not server thread count.

### Day-one rule for deterministic work

Within one partition tick, execution should be single-threaded unless a subsystem is specifically designed for deterministic internal parallelism.

That means:

- no concurrent mutation inside the same partition by default,
- parallelism happens across partitions first,
- only introduce intra-partition parallel jobs later when the merge rules are explicit and deterministic.

This is the safer path for factory simulation. Cross-partition parallelism buys a lot before intra-partition parallelism becomes necessary.

### Synchronization model

The system should synchronize through published snapshots and message queues, not through shared live object graphs.

From day one:

- network runtime queues validated commands into partition mailboxes,
- simulation workers consume commands at authoritative scheduled ticks,
- cross-partition transfers are written into boundary queues and applied on defined tick boundaries,
- projection extracts compact read models from simulation state,
- renderer consumes the latest published projection snapshot,
- persistence jobs read versioned snapshots or append-only logs rather than poking live runtime state.

That keeps contention low and makes it possible to reason about correctness.

### Server-side partition scheduling policy

The scheduler should make partition execution deterministic without requiring a world-global stall.

Recommended policy:

- each partition has an authoritative tick and readiness state,
- ready partitions can advance independently on the worker pool,
- cross-partition traffic is materialized only through explicit boundary messages,
- slow or blocked clients do not freeze unrelated partitions,
- hot partitions can be rebalanced or pinned deliberately,
- replication and snapshot generation happen after authoritative state advancement, not during random mid-step reads.

### Client-side mirrored scheduling policy

The client simulation executor should mirror the server's partition contract while remaining free to scale its local worker usage up or down.

Recommended policy:

- start with one simulation worker or a very small worker pool,
- represent each mirrored partition as a logical lane with its own sync state,
- let the executor multiplex multiple lanes onto the same worker when load is modest,
- promote especially hot or nearby partitions to dedicated workers only when profiling justifies it,
- drop a partition back to observational mode when the client cannot keep it inside the lag window.

This is a better default than forcing one worker per mirrored partition from the beginning.

### What this means for code structure

The rewrite should encode threading boundaries into the module, library, and target layout.

That implies separate libraries or modules for:

- simulation scheduler and partition executor,
- render backend and render-thread transport,
- network runtime and protocol lanes,
- projection extraction and frame publication,
- snapshot, replay, and persistence pipelines.

Do not let gameplay systems assume direct access to render state, socket state, or neighboring partition memory. If those assumptions leak into APIs early, the engine will fight multithreading forever.

### First implementation bar

The first usable native version should already prove all of the following:

- server partitions run as isolated logical lanes on a worker pool,
- client simulation runs off the UI or host thread,
- render consumes published snapshots instead of live runtime state,
- cross-partition transfer uses explicit queues,
- deterministic correctness does not depend on matching thread counts between client and server.

### Actor runtime

This can still be ECS-like, but keep it small and honest about its role.

Use it for:

- player movement,
- NPC state,
- transient world entities,
- camera targets,
- scene events,
- debug tooling,
- render-facing composition.

This is where the spirit of `createSystem` belongs.

### Factory runtime

This should not be an ECS just because the current POC used ECS.

Use SoA arrays, adjacency tables, partition-local handles, and compact item buffers. Build storage that matches the simulation workload.

Examples:

- belts as lane buffers and topology references,
- machines as indexed state arrays,
- grids and spatial lookups as direct tables or chunk-local maps,
- IO boundaries as versioned queues between partitions.

### Projection layer

This is where the pre-investigation notes add useful structure.

The projection layer should translate authoritative state and simulation outputs into client-facing read models without forcing the underlying runtimes to share one storage model.

Responsibilities:

- translate low-churn replicated actor state into render-friendly or UI-friendly state,
- translate deterministic factory simulation outputs into compact visual buffers,
- smooth small corrections and preserve visual continuity,
- maintain debug and inspection views,
- optionally feed a lightweight presentation ECS where that improves tooling or UX.

In other words, ECS can remain a projection of simulation on the client, but it should not be the simulation substrate for factory workloads.

### Renderer

Use one renderer, but do not force one world model.

The renderer should consume extracted buffers or snapshots from the projection layer and build GPU-friendly batches. The factory runtime should be able to render ten thousand belt items without creating ten thousand general-purpose entities.

## Networking Foundation

### Core model

The networking model should be authoritative server plus mirrored deterministic clients for relevant partitions.

That means:

- the server is always authoritative,
- clients never submit authoritative state,
- clients may mirror deterministic partitions locally,
- clients may render partition results from their local mirror,
- the server corrects by snapshot/resync, not by trusting client output.

### Commands, events, patches, and snapshots

The pre-investigation notes were correct that bandwidth collapses when the system ships fully resolved high-churn state every tick. The adjustment is that the architecture needs more than one message shape.

Use:

- typed commands for gameplay intent and authoritative scheduling,
- explicit boundary events for cross-partition transfer such as trucks, trains, and bulk IO,
- narrow replicated patches for low-churn actor state,
- snapshots for hydrate, bootstrap, late join, reconnect, and resync.

Do not stream fully resolved per-tick factory state such as every carried item position on every belt.

### How the two networking lanes coexist

#### Lane A: low-churn replication

Use for:

- players,
- NPCs,
- ownership/visibility state,
- login/session state,
- other data that is cheap to replicate and cheap to correct.

This lane can remain ECS-adjacent if that is ergonomically useful.

#### Lane B: deterministic partition commands

Use for:

- build commands,
- delete commands,
- machine configuration changes,
- logistics commands,
- other gameplay intent that should execute at an authoritative partition tick.

Suggested flow:

1. Client sends typed intent.
2. Server validates intent.
3. Server assigns authoritative partition tick.
4. Server broadcasts scheduled command to mirrored participants.
5. Participants confirm readiness for that partition timeline.
6. Server commits the command.
7. All mirrored participants execute the same command on the same partition tick.
8. Hashes and progress metadata detect drift.
9. Snapshots recover divergence.

Inside this lane, network traffic should center on intent, scheduling, boundary transfer events, and rare correction or recovery paths rather than resolved per-tick movement state.

### Client sync and degradation policy

The repo-backed design already defines `hydrating`, `catching-up`, `live`, `desynced`, and `resyncing` states. The external notes add one useful framing on top: clients should mirror only as much deterministic simulation as they can afford.

Recommended client policy:

- mirrored mode for partitions the client can keep inside the target lag window,
- catch-up mode with an explicit work budget when the client falls behind,
- an observational fallback for partitions the client cannot currently mirror safely,
- re-entry through hydration and catch-up once the partition becomes healthy again.

The observational fallback should be treated as a sync policy layered above the protocol, not as a separate authoritative model. In that mode, the client stops executing that partition locally and consumes authoritative projection data until it can resume mirrored execution.

Selective local optimism is still fine for UX-sensitive paths such as movement feel, build previews, or immediate visual response, but rollback-heavy client prediction should not be the default contract for factory simulation. Prefer authoritative future-tick scheduling plus small projection-layer smoothing over mandatory rewind and replay for every partition command.

### Important MMO-specific rule

Do not freeze the whole world because one client is behind.

If a mirrored client falls behind:

- the server keeps the partition authoritative,
- the client catches up or resyncs,
- only that partition and that client's mirrored view should be affected.

This is why the current repo's partition-local readiness model is the correct direction for an MMO rather than whole-world lockstep.

### Cross-partition IO

Treat claim boundaries as explicit IO interfaces.

Examples:

- truck transfer,
- train exchange,
- logistics transfer,
- bulk transfer systems,
- market transactions,
- long-distance power or fluid interfaces,
- cross-claim automation handoff.

Do not allow hidden arbitrary reads across partition boundaries. That destroys scalability and determinism. These boundaries should behave like explicit transfer contracts, not like one continuous simulation graph stretched across the whole MMO world.

## Build, Tooling, and Common C++ Structure

### Build system

Use CMake with CMakePresets and Ninja.

Suggested baseline:

- `cmake --preset debug`
- `cmake --build --preset debug`
- `ctest --preset debug`
- `cmake --preset release`
- `cmake --build --preset release`
- `cmake --preset profiling`
- `cmake --build --preset profiling`

Recommended additions:

- `clang-format` for formatting,
- `clang-tidy` for static analysis,
- AddressSanitizer, UndefinedBehaviorSanitizer, and ThreadSanitizer build presets,
- `ccache` or `sccache` for compilation caching,
- `lld` or `mold` for faster linking,
- a thin `justfile` or task runner script if command aliases become noisy,
- `vcpkg` or Conan only if dependency management actually becomes painful enough to justify it.

### Common workspace structure

One reasonable layout:

```text
CMakeLists.txt
CMakePresets.json
justfile
cmake/
external/
src/
  apps/
    client/
    server/
    tools/
  engine/
    runtime/
    render/
    assets/
    scheduler/
    hotreload/
  gameplay/
    actor-runtime/
    sim-core/
    sim-claims/
    sim-io/
  networking/
    protocol/
    runtime/
  persistence/
    save-format/
  foundation/
    math/
    diagnostics/
    memory/
  plugins/
    player-movement/
    building/
    debug-overlay/
tests/
benchmarks/
justfile
```

### How C++ projects commonly stay manageable

The useful convention is not "one giant engine library."

The useful convention is:

- one top-level CMake project,
- many focused libraries by domain,
- small executable targets for apps and tools,
- public headers kept narrow and intentional,
- modules grouped by feature rather than inheritance trees,
- explicit ownership and explicit data flow.

That shape fits this game much better than trying to port the current monorepo structure literally.

## Hot Reload Strategy

### Yes, but only with strict boundaries

You can absolutely build a system where gameplay systems are compiled as reloadable native modules, but the boundary has to be designed from day one.

The current TypeScript engine already has the right conceptual seam in [src/engine/src/core/system/index.ts](../../src/engine/src/core/system/index.ts) and [vite/engine-hmr/runtime.ts](../../vite/engine-hmr/runtime.ts): swap behavior in place while preserving live state.

The native rewrite should preserve that idea.

### Recommended native hot reload model

Use a host/plugin split.

The host runtime owns:

- world memory,
- allocators,
- scheduler,
- asset services,
- network state,
- renderer,
- long-lived simulation state.

Hot-reloadable modules own:

- behavior,
- callbacks,
- validation rules,
- system registration,
- render extractors,
- debug tools.

In C++, compile hot-swappable modules as shared libraries with a narrow C ABI boundary or a tightly versioned plugin ABI and load them through a runtime loader.

Relevant linkage note: prefer an explicit exported factory surface over exposing wide C++ class hierarchies directly across module boundaries. That keeps reload behavior, binary compatibility, and ownership rules tractable.

### Practical rules for native hot reload

- Keep host-owned state opaque to plugins.
- Pass handles, views, command buffers, or function tables across the boundary instead of raw internal references.
- Version the plugin API explicitly.
- Make reloadable modules disposable and re-bindable.
- Separate dev-time hot reload from shipping builds. In production, prefer static linking.

### What should not be hot reloaded

Avoid hot reloading these without a real migration layer:

- core memory layouts,
- save-format schema,
- network protocol schema,
- deterministic state structs,
- low-level renderer internals,
- allocator contracts.

### Alternative: WASM modules for some gameplay layers

If native DLL reload becomes too fragile, a secondary option is to use WASM for selected gameplay or tool modules while keeping the authoritative core native.

That gives you:

- a cleaner sandbox boundary,
- easier cross-platform loading,
- safer reload behavior.

It costs:

- more host-call overhead,
- tighter ABI constraints,
- less raw performance for hot-path simulation.

For this project, native dynamic modules are the better default for dev-time system reload, but WASM is worth keeping in reserve for tool or scripting layers.

## Suggested First Rewrite Milestones

### Phase 1. Build the deterministic core first

Do this before rebuilding the entire renderer stack.

Build:

- fixed-step partition runtime,
- partition scheduler plus worker-pool execution,
- explicit boundary mailboxes between partitions,
- command scheduling,
- snapshot save/load,
- replay hash tests,
- headless server execution.

### Phase 2. Prove hybrid networking

Build:

- low-churn actor replication lane,
- deterministic simulation lane,
- client-side simulation worker or small worker pool,
- catch-up and resync flow,
- partition-local readiness and hash verification.

### Phase 3. Attach a renderer to extracted state

Build one renderer that reads:

- published latest-wins render snapshots,
- actor extracts,
- factory extracts,
- debug overlays.

Do not rebuild the renderer around simulation ownership assumptions. Prove the render-thread handoff before layering on content complexity.

### Phase 4. Reintroduce high-level ergonomics

Recreate the good parts of this repo once the low-level truth is stable:

- system registration API,
- asset registry ergonomics,
- pooled transient memory helpers,
- editor/dev tooling,
- hot reload for selected modules.

## Final Recommendation

The correct move is not to evolve this repository indefinitely.

The correct move is to treat it as a successful architectural prototype that already answered the hard question: this game wants a hybrid engine, not a pure ECS engine.

Build the next codebase in C++.

Keep:

- the system ergonomics,
- the update/render separation,
- the asset ownership model,
- the pooling mindset,
- the type discipline,
- the hybrid networking conclusion.

Throw away:

- JS as the long-term authoritative simulation runtime,
- ECS diff replication as the main gameplay protocol,
- the idea that factory simulation should be modeled like player/world actor state.

Start from scratch on:

- deterministic partition simulation,
- native networking runtime,
- render extraction architecture,
- hot-reload boundary,
- persistence/replay/resync pipeline.

The POC was worth it. It gave you the shape of the real engine.

## Source References From This Repository

- [17-DETERMINISTIC-PARTITIONED-SIMULATION-ROADMAP.md](./17-DETERMINISTIC-PARTITIONED-SIMULATION-ROADMAP.md)
- [18-HYBRID-COMMAND-NETWORKING-PROTOCOL.md](./18-HYBRID-COMMAND-NETWORKING-PROTOCOL.md)
- [conveyor-system-design.md](../conveyor-system-design.md)
- [RENDERING_50K_120FPS_ROADMAP.md](../RENDERING_50K_120FPS_ROADMAP.md)
- [RENDERING_1M_ENTITY_AUDIT.md](../RENDERING_1M_ENTITY_AUDIT.md)
- [10-FEATURE-RENDER-THREADING.md](./10-FEATURE-RENDER-THREADING.md)
- [ASSET_MANAGEMENT.md](../ASSET_MANAGEMENT.md)
- [08-PLUGIN-BASED-IMPLEMENTATION.md](./08-PLUGIN-BASED-IMPLEMENTATION.md)
- [src/engine/src/core/system/index.ts](../../src/engine/src/core/system/index.ts)
- [src/engine/src/asset/AssetManager.ts](../../src/engine/src/asset/AssetManager.ts)
- [src/utils/src/pool.ts](../../src/utils/src/pool.ts)
- [vite/engine-hmr/runtime.ts](../../vite/engine-hmr/runtime.ts)