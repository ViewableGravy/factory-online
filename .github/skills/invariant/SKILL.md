---
name: invariant
description: Use runtime assertions and explicit checks instead of casts or null-suppression when a value must exist.
---

# Invariant Usage Skill

Use this skill whenever you need to assert that a value exists or that a precondition holds at runtime.

## Core Rules

1. Prefer runtime assertions over type or cast-based escape hatches.
   - Use the project's assertion mechanism, `assert`, or an explicit guard that fails loudly.
   - Avoid using casts to hide missing invariants.
2. Assert the value or contract directly.
   - Prefer checking the pointer, handle, or condition that actually matters.
3. Use explicit guards when zero, empty, or false are valid values.
   - Do not treat all falsy-looking values as failures when the domain allows them.

## Patterns

### Replace downstream defensive plumbing

```cpp
Entity* entity = world.find(entityId);
assert(entity && "entity must exist after lookup ownership was established");
entity->tick();
```

### Guard when a zero-like value is valid

```cpp
const auto index = map.find(key);
if (index == map.end()) {
    throw std::runtime_error("Index missing for key");
}
```

## Mindset

- Assertions document ownership and expected program shape.
- A required value should fail close to the boundary that required it.
- Do not normalize impossible states into broad optional flows just to quiet the compiler.