---
name: engine-render-philosophy
description: Philosophy and decision rules for authoritative update logic versus read-only rendering in the native runtime.
---

# Engine Render Philosophy

## Purpose

Document the intended separation between simulation and rendering so authoritative state and presentation logic do not bleed into each other.

## When to use

- Adding or refactoring engine, gameplay, or runtime systems.
- Adding or refactoring renderer stages, frame extraction, or draw preparation.
- Reviewing code that mixes simulation state with visual presentation.

## Behavior

1. Treat update as the place where the world advances to its next authoritative state.
2. Treat render as a read-only consumer of that state whose job is to project it onto the screen.
3. Keep render work observational.
   - Derive transient visual output from current inputs.
   - Do not write persistent world state during render.

## Core split

- Update owns simulation, gameplay, physics, world mutation, orchestration, timers, and persistent transitions.
- Render owns presentation, interpolation, extraction, draw preparation, frame-local culling, and screen output.
- If a behavior changes what the world is, it belongs in update.
- If a behavior changes only how the current world state is shown, it belongs in render.

## Render rules

- Render is read-only with respect to authoritative gameplay state.
- Render must not create hidden gameplay state, mutate entity ownership, or patch simulation data before drawing.
- Render may derive transient values such as interpolated transforms, blended colors, per-frame uniforms, or draw lists.
- Prefer frame-local scratch data over persistent render-owned state.

## Decision test

- Ask: "Would saving and reloading the world need this value?"
- If yes, it belongs in update-owned state.
- If no, and it only affects presentation for the current frame, it can be derived during render.

## Review guidance

- If render code needs to fix simulation state before drawing, the ownership boundary is wrong.
- If a system exists only to precompute presentation data, confirm whether render should derive it directly.
- When in doubt, move persistent decision-making earlier into update and keep render as a pure read path.