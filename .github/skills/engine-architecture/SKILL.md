---
name: engine-architecture
description: Architectural patterns and conventions for the Java runtime and gameplay code in this repository.
---

# Engine Architecture

## Purpose

Capture the architectural patterns worth preserving from the reference engine while implementing the runtime in Java.

## When to use

- Creating or refactoring runtime subsystems.
- Designing runtime APIs or gameplay modules.
- Reviewing whether a new abstraction fits the intended runtime architecture.

## Core patterns

1. Prefer focused managers or subsystems over a bloated root runtime class.
   - Good examples are scene management, asset ownership, scheduler control, input, or diagnostics.
   - Group related behavior behind a narrow domain surface instead of growing a god object.
2. Keep system orchestration separate from deep domain logic.
   - Entry points should read context, validate preconditions, and delegate to focused implementation code.
3. Preserve named lifecycle phases.
   - Initialization, update, render extraction, cleanup, and reload seams should be explicit.
4. Keep ownership explicit.
   - The runtime should clearly own registries, subsystem lifetimes, thread boundaries, and mutable state transitions.
5. Prefer optional modules and focused packages over baking every feature into the lowest layer.

## Design guidance

- Favor feature-oriented packages over inheritance-heavy hierarchies.
- Keep public interfaces small and stable.
- Hide implementation details behind package-private types when they do not need to leak.
- When adding a new capability, first ask whether it belongs in an existing subsystem or a new focused manager.

## Reload and seams

- If reloadable behavior is supported later, keep the host or plugin boundary narrow.
- Preserve live state in the host where possible.
- Reload behavior modules, not arbitrary ownership graphs.

## What to avoid

- Do not grow the root runtime type into the primary home for unrelated feature methods.
- Do not hide thread ownership behind convenience APIs.
- Do not make render code responsible for repairing simulation state.