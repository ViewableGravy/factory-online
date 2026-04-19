---
name: skill-authoring
description: Standardized process for creating or updating agent skills in this repository.
---

# Skill Authoring Standard

Use this skill whenever creating or updating any skill under `.github/skills`.

## Required workflow

1. Keep the skill narrowly scoped.
   - Each skill should solve one recurring problem or encode one stable convention set.
2. Write a short frontmatter block with a clear `name` and `description`.
3. Include these sections when they help:
   - Purpose
   - When to use
   - Rules, workflow, or patterns
4. Keep the skill self-contained.
   - Do not depend on unstated tribal knowledge.
5. Keep examples aligned with the root repository's target stack.
   - Prefer C++, CMake, testing, architecture, and debugging examples over TypeScript-specific ones unless the skill is explicitly scoped to the reference repo.

## Quality bar

- Skills should be concise, specific, and reusable.
- Prefer durable guidance over temporary project trivia.
- If a skill contains commands, they should be executable or directly adaptable in this repository.