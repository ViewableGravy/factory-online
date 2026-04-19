---
name: code-structure
description: Preferred C++ file and module structure conventions for implementation work.
---

# Code Structure

## Purpose

Capture preferred file and folder structure conventions for future implementation work.

## File organization

- Keep files in a predictable order:
  1. includes
  2. forward declarations or local aliases
  3. constants and internal helpers
  4. type definitions
  5. function or class implementation
- Keep public headers narrow.
- Avoid leaking internal headers through public interfaces unless required.

## Header and source split

- Use headers for declarations and contracts.
- Use source files for implementation details and heavy includes.
- Keep inline implementation in headers only when templates, constexpr logic, or trivial accessors justify it.

## Module goals

- Keep orchestration files concise.
- Move domain-heavy logic into neighboring helpers or focused modules.
- Prefer feature folders over utility dumping grounds.

## State and constants

- Put stable compile-time constants close to the owning module.
- Avoid namespace-scope mutable state unless the ownership and lifetime are explicit and justified.
- Prefer state owned by a runtime object, manager, or subsystem over top-level globals.

## Splitting strategy

- If a module stays small and cohesive, keep it together.
- If a file starts mixing API surface, state management, simulation logic, and unrelated helpers, split it by responsibility.
- If a subsystem has a clear public seam, prefer a folder with a focused header and a small set of implementation files.