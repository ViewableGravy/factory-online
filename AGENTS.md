# AGENTS.md

## Purpose

This file is the root guidance for agent behavior in this repository.

The active implementation target for this workspace is Java under `java/`. The nested `better-ecs-reference/` repo remains a useful architectural reference, but its TypeScript, Bun, React, and Nx-specific rules should not be treated as the default for work in this root repository unless a task explicitly targets that folder.

## Workspace map

```text
java/                   -> Active Java runtime/application work
documentation/          -> In-repo exports and pointers; canonical long-form knowledge now lives in the Obsidian vault
better-ecs-reference/   -> Reference implementation and design source, not the default implementation target
.github/skills/         -> Repo-local skills for agentic programming in this workspace
```

## Obsidian vault

- Canonical vault location on Windows: `C:\Users\lleyt\factory-vault`
- Canonical vault location from WSL: `/mnt/c/Users/lleyt/factory-vault`
- Use the WSL path for all agent operations on the vault. Obsidian should open the Windows path natively.
- Treat the vault as the source of truth for repository knowledge, architecture notes, standards, ongoing investigation notes, and task coordination.
- Put durable knowledge and actionable work directly into the vault instead of leaving it only in transient chat context.
- File operations on `/mnt/c/` are slower than native Linux paths. That is acceptable for markdown knowledge work.

## Vault task workflow

- Use `todo/` inside the vault as the shared planning area.
- Create area-board notes for major surfaces such as networking, assets, rendering, architecture, persistence, and tooling.
- Area-board notes should act like simple kanban boards with sections such as Backlog, Ready, In Progress, Blocked, and Done.
- Store detailed task notes under `todo/tasks/` and link them from the relevant area-board notes.
- Each task should either capture durable repository knowledge or coordinate concrete work that still needs to happen.
- When an in-repo document is superseded by vault content, leave a short pointer in the repository rather than maintaining two competing long-form sources.

## Working process

- Start from the most local implementation surface or failing behavior.
- Use targeted search before broad repo exploration.
- Reuse or extend existing code before introducing new helpers or abstractions.
- Treat the Obsidian vault as the main architectural and planning reference when runtime decisions are unclear.
- Create or update vault notes when work uncovers durable guidance, repository standards, or follow-up tasks.
- Use sub-agents for broad investigation or repository search when that is cheaper than carrying the full context in the main thread.

## Reference repo policy

- Read `better-ecs-reference/` for concepts, patterns, and lessons.
- Do not copy TypeScript, CSS, React, Bun, Vite, or Nx-specific conventions into the root repo unless the current task explicitly works inside that nested repo.
- When a pattern is worth keeping, port the intent rather than the syntax.

## Runtime architecture rules

- Preserve the update-versus-render split.
	- Update owns authoritative simulation and persistent state transitions.
	- Render is observational and should not mutate world state.
- Prefer explicit ownership boundaries.
	- Use deterministic cleanup.
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

## Java coding standards

- Prefer guard clauses over nested `else` trees.
- Keep interfaces narrow and composable.
- Prefer immutable values and explicit ownership boundaries by default.
- Avoid unnecessary shared mutable state.
- Keep comments that explain ownership, invariants, determinism constraints, or thread assumptions.
- Remove dead code rather than keeping compatibility shims unless the task explicitly requires a migration layer.

## Assertions and invariants

- Prefer explicit runtime assertions at ownership boundaries over nullable plumbing spread throughout the call graph.
- Do not use casts to paper over missing invariants unless there is no better option.
- If a value must exist by contract, assert once at the boundary and continue with a non-nullable path.

## Tooling and verification

- Standard build flow from `java/`:
	- `make build`
	- `make run`
- Use narrower validation first for touched code, then widen as needed.
- If formatting or static analysis is configured, use the narrowest relevant Java tooling on touched code.

## File and module design

- Prefer feature-oriented folders and packages.
- Keep public APIs focused and minimize transitive dependencies.
- Split modules by domain behavior instead of accumulating unrelated helpers in one file.

## Skill usage

- Check `.github/skills/` before improvising repo-local conventions.
- Use the Obsidian vault workflow skill when work involves documentation capture, task coordination, or knowledge-base updates.
- Prefer the repo-local skill over generic habits when they conflict.

## Completion bar

- Do not treat a task as done until the touched code has been validated with the narrowest relevant build, test, or static-analysis command available.
- If validation could not be run, state exactly what remains unverified.
