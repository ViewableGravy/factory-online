---
name: coding-guidelines
description: General Java and runtime-facing code-writing preferences for this repository. Use when implementing or refactoring code so changes stay readable, explicit, and compatible with the active runtime direction.
---

# Coding Guidelines

## Purpose

Capture recurring code-style preferences that should shape day-to-day implementation work in this repository.

## When to use

- Writing new runtime, gameplay, tooling, or infrastructure code.
- Refactoring existing code while preserving behavior.
- Reviewing implementation structure before finalizing edits.

## Guidelines

1. Avoid clever control flow when a direct branch reads better.
   - Prefer `if` statements and guard clauses over nested ternaries or compressed logic.
2. Keep code readable for someone with less context than the original author.
   - Use clear names, targeted helper methods, and short comments where they remove ambiguity.
3. Make ownership and mutation obvious.
   - The reader should be able to see whether a method mutates shared state, returns a derived value, or delegates work.
4. Push invariants to the boundary that establishes ownership.
   - Assert once where a value becomes required rather than spreading null checks through downstream code.
5. Prefer explicit data flow over hidden global mutation.
   - Pass state through parameters, handles, or context objects instead of reaching through ambient singletons.
6. Keep hot-path code allocation-aware.
   - Avoid incidental object churn in per-frame, per-tick, or inner-loop paths.