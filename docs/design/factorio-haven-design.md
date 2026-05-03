# Factorio + Haven & Hearth — Design & Roadmap

## Executive Summary

Goal: Build a single-authoritative server MMO combining Factorio-style automation with Haven & Hearth-style single persistent world and land claims. Landclaims are first-class simulation boundaries (one simulation per landclaim). Non-simulated IO (trucks/harvesters) operate via dedicated systems or message handoffs. MVP focuses on multiplayer connectivity, player entities, a global grid, client prediction for movement, ASCII rendering for rapid iteration, and basic persistence.

## Core Inspirations

- **Haven & Hearth**
  - Persistent single shared world, land ownership/claims, social and emergent mechanics.
  - Design decision: single authoritative world and persistent land ownership.

- **Factorio**
  - Grid-based factories, deterministic machine and item simulations within local areas.
  - Design decision: treat factories as bounded, simulation-contained systems inside landclaims.

Combine those ideas: single shared world with grid-chunks; landclaims map to sets of chunks and run isolated simulations to simplify threading and ownership.

## High-level Architecture

- **Authoritative server**: single process for the world state and persistence.
- **Landclaim = SimulationBoundary**: each claim runs a SimulationWorker (thread or scheduled task) owning local entities, machines, belts, and item flows.
- **Global coordinate system**: integer 2D grid (x,y), optional z/layer if needed. No local→world transforms.
- **Chunking**: fixed-size chunks (e.g., 32×32 or 64×64) partition the grid and map to landclaims.
- **Entities**: Player, Machine, Transporter, ItemStack, StaticTile, etc.
- **Networking**: server-authoritative; clients send inputs, server applies authoritative updates and sends diffs.
- **Non-simulated IO**: trucks/harvesters implemented as separate worker systems or special transport workers performing transactional handoffs at claim borders.

## Coordinate System & Chunking

- Single global integer grid avoids complex transforms.
- Chunk type provides containment, persistence unit, and streaming unit for clients.
- Landclaim metadata includes owner(s), chunk set, and simulation handle.

## Landclaims as Simulation Boundaries

- A landclaim owns and runs its own simulation (machines, item routing, local entities).
- SimulationWorker model:
  - Owns the entity list for the claim.
  - Runs per-claim tick updates.
  - Publishes state diffs for network streaming and persistence.
- Cross-claim interactions happen through explicit gateway tiles or transport worker handoffs.

Benefits: reduced lock contention, easier threading, local debugging and checkpointing.

## Player Movement & Non-determinism

- Players are non-deterministic relative to the machine simulations.
- Use **client prediction** and **server reconciliation**:
  - Client: locally predict movement and render immediately.
  - Server: authoritative position; sends corrections when needed.
- Tick model: server tick (e.g., 20–60 Hz). Inputs sampled and applied per-tick.

## Networking & Sync Strategy

- Minimal message types for MVP: Hello/Auth, Spawn, PlayerInput, ServerTick, EntityUpdate, WorldDiff, ClaimUpdate.
- Connect flow:
  - Client: Hello → server returns initial snapshot for visible chunks.
  - Runtime: client sends PlayerInput(seq,t), server sends authoritative tick diffs and entity updates.
- Sync approach:
  - Snapshot on connect for visible area.
  - Deltas for changed entities and tiles each tick or on change.
  - Interpolation on client to smooth corrections.

Example wire (pseudo-JSON):

```json
{"type":"PlayerInput","seq":42,"clientTime":1683070000,"move":{"dx":1,"dy":0}}
{"type":"EntityUpdate","id":123,"pos":{"x":10,"y":5},"ts":1683070000}
```

Decisions for MVP: start with reliable TCP+JSON for simplicity; evolve to compact/binary or UDP later if needed.

## Persistence

- Persist at chunk/landclaim granularity.
- Use periodic checkpoints and on-claim-save to reduce lost work on crash.
- Keep ownership and metadata separately for quick lookups.

## Threading & Concurrency

- Main server: accept connections and orchestrate scheduling.
- Worker pool: run SimulationWorkers (one per landclaim or a configurable group).
- Communication via message queues to minimize locking.
- Global registries sharded or narrow-synchronized when needed.

## Rendering & Client UX

- MVP: ASCII renderer for rapid iteration.
- Render is read-only: derive interpolated transforms from authoritative cached state.
- Later: move to LWJGL/SDL when visuals are matured.

## Input System

- Client bundles inputs (move/action) with timestamps and sequence ids.
- Server validates inputs and applies them to authoritative player state.
- Compress inputs to reduce chattiness.

## Security & Authority

- Server validates critical actions: claim creation, inventory changes, placements.
- Basic anti-cheat: reject impossible move speeds; authoritative reconciliation will correct clients.

## MVP Roadmap & Priorities

1. Multiplayer core (highest): server + clients, player entities, movement with reconciliation, ASCII client.
2. Global grid & chunking: canonical grid, chunk types, basic chunk persistence.
3. Landclaims & isolated simulation: claim creation, SimulationWorker per claim, machine simulation inside claim.
4. Persistence & streaming: chunk snapshots, diffs, streaming on connect.
5. Rendering upgrade: LWJGL or richer client when gameplay is stable.
6. Transport workers: prototype trucks and cross-claim handoffs.
7. Scaling & ops: profiling, sharding strategy if single-server limits are reached.

## Detailed First Milestone (Actionable Steps)

- Wire protocol: design minimal message schemas (JSON) for MVP.
- Server skeleton: TCP acceptor, session map, tick loop (20–30 Hz).
- Player entity + input pipeline: authoritative movement and per-tick application.
- ASCII client: connect, capture WASD, send inputs, locally predict movement, render small view.
- Manual test: 2 players connected and moving in shared ASCII world.

## Message & Data Schemas (Suggestions)

- PlayerInput: `{seq:int, clientTime:long, move:{dx:int,dy:int}, actions:[]}`
- ServerTick: `{tick:int, authoritativeTime:long, ackSeq:int}`
- EntitySnapshot: `{id:int, type:string, pos:{x:int,y:int}, meta:{}}`
- WorldDiff: `{chunkId:int, modifiedEntities:[EntitySnapshot], tileChanges:[tileOps]}`

## Testing & Verification

- Unit tests for chunks, claims, and ownership rules.
- Integration test: multi-client connection harness (use existing Java integration tools in the repo).
- Manual test: run server and 2–3 clients, test movement and reconciliation.

## Risks & Mitigations

- Single-server scaling: profile early, optimize hot paths, chunking reduces per-worker CPU.
- Concurrency bugs: use message-passing and narrow ownership boundaries.
- Cross-claim IO complexity: prototype transport workers and transactional handoffs.
- Latency/feel: client prediction + interpolation, choose appropriate server tick.

## Repo-aligned Implementation Notes

- Follow `engine-architecture` SKILL: explicit subsystems, narrow public surfaces, lifecycle phases.
- Follow `code-structure` and `coding-guidelines` SKILLs: small types, clear ownership, allocation-aware hot paths.
- Follow `engine-render-philosophy` SKILL: render read-only, derive transient values at render stage.
- Follow `investigation` SKILL: reproduce issues, add regression tests, and verify fixes objectively.

## Suggested Immediate Technical Tasks (Concrete)

1. Define the wire protocol (JSON or compact binary).
2. Implement server skeleton with tick loop and session management.
3. Implement ASCII client with input capture and local prediction.
4. Implement grid/chunk types and `Player` entity with serialization.
5. Run 2-player manual test and iterate on reconciliation smoothing.

## Milestone Schedule (Rough)

- Week 0–1: Wireproto, server skeleton, ASCII client, simple movement.
- Week 2: Chunking, grid, persistence basics, landclaim metadata.
- Week 3–4: Landclaim SimulationWorker, cross-claim messaging, truck prototype.
- Month 2+: Rendering upgrades, QoL, scaling and ops improvements.

## Appendix: Simple Client/Server Input Loop (Pseudo)

- Client:
  - capture input frame → send `PlayerInput(seq++)`
  - update local predicted position
  - on `EntityUpdate` correction → apply smooth correction over N frames
- Server:
  - collect inputs per client per tick
  - apply to authoritative `Player` entity
  - broadcast `EntityUpdate` deltas

---

If you want, I can now scaffold the wire protocol spec or create a minimal server+ASCII client prototype. Tell me which to do next.

**Repository Alignment**

- **Java: Tick & Transport**: `java/apps/client/.../ClientApplication.java`, `java/apps/client/.../ClientRuntimeLoop.java`, `java/apps/server/.../ServerRuntimeLoop.java`, and `java/libs/foundation/.../TcpClientTransport.java` contain the existing tick loop, tick-sync, and transport primitives — directly aligned with the authoritative tick model in this design.
- **Protocol DTOs**: generated protocol classes (`build/generated/.../InitialSimulationStateDTO.java`, `SimulationUpdateDTO.java`) exist and provide a starting point for snapshot/delta messages.
- **TypeScript: Land-claim client**: full client-side land-claim implementation and utilities exist under `better-ecs-reference/src/app/client/src/entities/land-claim/` (LandClaim, LandClaimQuery, component, ghost, const). This demonstrates a mature client representation and placement logic for claims.
- **TypeScript: Build-mode types**: `better-ecs-reference/src/libs/commands/src/build-mode/types.ts` includes `"land-claim"` in `BuildModeItemType`, so placement/UX work is present.
- **Client assets & compiled bundles**: the `.nx` workspace data and `.nx/cache` builds contain compiled land-claim UI/engine code and many Vitest references (`LandClaimQuery.spec.ts`) — shows client tests and runtime assets are exercised in CI/workspace runs.
- **Docs referencing chunking**: `better-ecs-reference/docs/conveyor-system-design.md`, `performance-research/serialization-performance-investigation.md`, and architecture docs reference chunk-level simulation and chunked persistence; these align with chunked-persistence and chunk-as-unit recommendations.
- **Gaps / Work Needed**: I did not find a canonical server-side `LandClaim` simulation entity in the Java server code during this scan — server must implement claim metadata, claim→chunk mapping, and a SimulationWorker that owns per-claim entities and persistence. Cross-claim transport/hand-off systems also need server-side implementations.
- **Quick wins**: reuse Java tick/transport and DTOs for server authoritative ticks; reuse TypeScript client assets and placement logic for client UX; implement a server-side claim model and per-claim simulation worker to bridge both sides.

If you want, I can (A) append detailed file excerpts and line-citations for the listed files, (B) scaffold the server-side `LandClaim` model and SimulationWorker, or (C) scaffold the wire protocol spec next. Which do you want me to do?

**Repository Alignment**

- **Java: Tick & Transport**: `java/apps/server/.../ServerRuntimeLoop.java` and `java/libs/foundation/.../TcpClientTransport.java` provide existing tick loop, tick-sync, and transport primitives — directly aligned with the authoritative tick model in this design.
- **Protocol DTOs**: generated protocol classes (`build/generated/.../InitialSimulationStateDTO.java`, `SimulationUpdateDTO.java`) exist and provide a starting point for snapshot/delta messages.
- **Docs referencing chunking**: architecture and performance docs (`docs/architecture/*`, `performance-research/serialization-performance-investigation.md`, `better-ecs-reference/docs/conveyor-system-design.md`) reference chunk-level simulation and chunked persistence; these align with chunked-persistence and chunk-as-unit recommendations.
- **Gaps / Work Needed**: I did not find a canonical server-side `LandClaim` simulation entity in the Java server code during this scan — server must implement claim metadata, claim→chunk mapping, and a `SimulationWorker` that owns per-claim entities and persistence. Cross-claim transport/hand-off systems also need server-side implementations.
- **Quick wins**: reuse Java tick/transport and DTOs for server authoritative ticks; implement a server-side claim model and per-claim `SimulationWorker` to deliver the landclaim-as-simulation-boundary design.

If you want, I can (A) append detailed file excerpts and line-citations for the listed Java files, (B) scaffold the server-side `LandClaim` model and `SimulationWorker`, or (C) scaffold the wire protocol spec next. Which do you want me to do?