---
name: code-structure
description: Preferred Java file and package structure conventions for implementation work.
---

# Code Structure

## Purpose

Capture preferred file and package structure conventions for future implementation work.

## File organization

- Keep files in a predictable order:
  1. package declaration
  2. imports
  3. class-level constants and fields
  4. constructors
  5. public methods
  6. private helpers
- Keep public types narrow.
- Avoid exposing implementation details through broad public APIs unless required.

## Type and package split

- Use one top-level public type per file.
- Keep related package-private helpers near the owning feature when they do not need wider visibility.
- Split large responsibilities into neighboring classes instead of accumulating unrelated helpers in one type.

## Module goals

- Keep orchestration classes concise.
- Move domain-heavy logic into focused neighboring types.
- Prefer feature folders and packages over utility dumping grounds.

## State and constants

- Put stable constants close to the owning type.
- Avoid static mutable state unless ownership and lifetime are explicit and justified.
- Prefer state owned by a runtime object, manager, or subsystem over top-level globals.

## Splitting strategy

- If a type stays small and cohesive, keep it together.
- If a file starts mixing API surface, state management, simulation logic, and unrelated helpers, split it by responsibility.
- If a subsystem has a clear public seam, prefer a focused package with a small set of narrow types.