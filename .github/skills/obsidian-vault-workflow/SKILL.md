---
name: obsidian-vault-workflow
description: Use the Windows-hosted Obsidian vault as the source of truth for repository knowledge, standards, and task coordination.
---

# Obsidian Vault Workflow

## Purpose

Use the shared Obsidian vault for durable repository knowledge and task coordination instead of leaving important context only in chat or scattered repo docs.

## When to use

Use this skill when:

- documenting architecture, standards, or decisions,
- capturing durable investigation findings,
- creating or updating task boards,
- breaking large in-repo docs into more navigable vault notes,
- recording follow-up work that should survive beyond the current chat.

## Vault access

- Windows vault path: `C:\Users\lleyt\factory-vault`
- WSL vault path: `/mnt/c/Users/lleyt/factory-vault`
- Use the WSL path for agent operations.
- Obsidian should open the Windows path natively.
- File operations on `/mnt/c/` are slower than native Linux paths. That is expected and acceptable for markdown work.

## Rules

- Treat the vault as the source of truth for long-form repository knowledge and work tracking.
- Prefer updating the existing vault note structure over creating parallel notes with overlapping scope.
- When a repo markdown file is superseded by vault content, replace the repo file with a short pointer instead of maintaining two canonical copies.
- Keep notes durable and reusable. Remove chat-specific phrasing and temporary conversational context.
- Link overview notes to detail notes so humans and agents can navigate quickly.

## Todo workflow

- Use `todo/` as the shared planning area.
- Create top-level area boards such as `todo/Architecture.md`, `todo/Networking.md`, `todo/Assets.md`, `todo/Rendering.md`, `todo/Persistence.md`, and `todo/Tooling.md`.
- Treat each area board like a simple kanban board with sections for `Backlog`, `Ready`, `In Progress`, `Blocked`, and `Done`.
- Store task notes under `todo/tasks/` and link them from the relevant area boards.
- Every task note should either:
  - capture durable repository knowledge that needs to exist in the vault, or
  - coordinate concrete implementation, investigation, or cleanup work.

## Task note shape

Use a compact structure such as:

```md
# TASK-AREA-001 Short title
Status: Backlog
Area: Networking
Outcome: What this task should produce
Why: Why this matters
Repo citations:
- path/to/file
Next actions:
- action
```

## Recommended note structure

- Put long-form architecture and standards under `knowledge/`.
- Use index notes to split large topics into smaller linked notes.
- Keep supporting repo citations in the note when they help future review.
- Prefer one topic per note when a file grows large enough to be hard to scan in Obsidian.
