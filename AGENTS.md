# AGENTS.md

## Purpose

This file is the root guidance for agent behavior in this repository.

The long-term target for this workspace is a native C++ codebase. The nested `better-ecs-reference/` repo is a useful architectural reference, but its TypeScript, Bun, React, and Nx-specific rules should not be treated as the default for work in this root repository unless a task explicitly targets that folder.

## Workspace map

```text
cpp/                    -> Native runtime and engine work should live here as the C++ codebase grows
documentation/          -> Source of truth for rewrite architecture and native runtime direction
better-ecs-reference/   -> Reference implementation and design source, not the default implementation target
.github/skills/         -> Repo-local skills for agentic programming in this workspace
```

## Working process

- Start from the most local implementation surface or failing behavior.
- Use targeted search before broad repo exploration.
- Reuse or extend existing code before introducing new helpers or abstractions.
- Treat `documentation/core.md` as the main architectural reference when native runtime decisions are unclear.
- Use sub-agents for broad investigation or repository search when that is cheaper than carrying the full context in the main thread.

## Reference repo policy

- Read `better-ecs-reference/` for concepts, patterns, and lessons.
- Do not copy TypeScript, CSS, React, Bun, Vite, or Nx-specific conventions into the root repo unless the current task explicitly works inside that nested repo.
- When a pattern is worth keeping, port the intent rather than the syntax.

## Native architecture rules

- Preserve the update-versus-render split.
	- Update owns authoritative simulation and persistent state transitions.
	- Render is observational and should not mutate world state.
- Prefer explicit ownership boundaries.
	- Use RAII and deterministic cleanup.
	- Avoid hidden global state and ambiguous lifetime sharing.
- Prefer focused domain modules over one giant engine library.
- Keep simulation deterministic where gameplay depends on it.
	- Fixed-step updates.
	- Stable ordering.
	- Deterministic random sources.
	- No gameplay dependence on render timing.
- Treat thread boundaries as explicit API boundaries.
	- No casual shared mutable state across threads.
	- Prefer message passing, job inputs/outputs, or single-writer ownership.

## C++ coding standards

- Prefer guard clauses over nested `else` trees.
- Keep interfaces narrow and composable.
- Prefer value types and stack ownership by default.
- Use `std::unique_ptr` for exclusive heap ownership and `std::shared_ptr` only when shared lifetime is truly required.
- Prefer `std::span`, references, and views when ownership should not transfer.
- Avoid raw `new` and `delete` in application code.
- Avoid hidden allocations in hot paths.
- Keep comments that explain ownership, invariants, determinism constraints, or thread assumptions.
- Remove dead code rather than keeping compatibility shims unless the task explicitly requires a migration layer.

## Assertions and invariants

- Prefer explicit runtime assertions at ownership boundaries over nullable plumbing spread throughout the call graph.
- Do not use casts to paper over missing invariants unless there is no better option.
- If a value must exist by contract, assert once at the boundary and continue with a non-nullable path.

## Tooling and verification

- Prefer CMake with CMakePresets and Ninja.
- Standard build flow from `documentation/core.md`:
	- `cmake --preset debug`
	- `cmake --build --preset debug`
	- `ctest --preset debug`
- Use narrower validation first for touched code, then widen as needed.
- When debugging memory or threading issues, prefer sanitizer-enabled builds if presets are available.
- If formatting or static analysis is configured, use `clang-format` and `clang-tidy` on touched code.

## File and module design

- Prefer feature-oriented folders and libraries.
- Keep headers focused and minimize transitive includes.
- Separate interface from implementation when that meaningfully improves compile-time hygiene or API clarity.
- If a module grows large, split by domain behavior instead of accumulating unrelated helpers in one file.

## Skill usage

- Check `.github/skills/` before improvising repo-local conventions.
- Prefer the repo-local skill over generic habits when they conflict.

## Completion bar

- Do not treat a task as done until the touched code has been validated with the narrowest relevant build, test, or static-analysis command available.
- If validation could not be run, state exactly what remains unverified.
