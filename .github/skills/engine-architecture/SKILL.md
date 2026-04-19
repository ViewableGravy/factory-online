---
name: engine-architecture
description: Architectural patterns and conventions for a native C++ engine and gameplay runtime in this repository.
---

# Engine Architecture

## Purpose

Capture the architectural patterns worth preserving from the reference engine while translating them into a native C++ codebase.

## When to use

- Creating or refactoring engine subsystems.
- Designing runtime APIs or gameplay modules.
- Reviewing whether a new abstraction fits the intended native architecture.

## Core patterns

1. Prefer focused managers or subsystems over a bloated root engine class.
   - Good examples are scene management, asset ownership, scheduler control, input, or diagnostics.
   - Group related behavior behind a narrow domain surface instead of growing a god object.
2. Keep system orchestration separate from deep domain logic.
   - Entry points should read context, validate preconditions, and delegate to focused implementation code.
3. Preserve named lifecycle phases.
   - Initialization, update, render extraction, cleanup, and hot-reload seams should be explicit.
4. Keep ownership explicit.
   - The runtime should clearly own memory, handles, registries, and subsystem lifetimes.
5. Prefer optional modules and plugins over baking every feature into the lowest layer.

## Design guidance

- Favor feature-oriented libraries over inheritance-heavy hierarchies.
- Keep public interfaces small and stable.
- Use internal implementation details that cannot be fabricated casually by external code.
- When adding a new engine capability, first ask whether it belongs in an existing subsystem or a new focused manager.

## Hot reload and seams

- If hot reload is supported, keep the host/plugin boundary narrow.
- Preserve live state in the host where possible.
- Reload behavior modules, not arbitrary ownership graphs.

## What to avoid

- Do not grow the root engine type into the primary home for unrelated feature methods.
- Do not hide thread ownership or allocator ownership behind convenience APIs.
- Do not make render code responsible for repairing simulation state.