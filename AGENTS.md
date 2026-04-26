# Workflow
- NEVER remove code comments unless the user explicitly mentions to
- NEVER replace hard coded calls with loops unless explicitply asked to

# Agentic Workflow
## Request to Completion workflow (MUST FOLLOW FOR ALL REQUESTS)
1. Use AskQuestions MCP to confirm edge cases and unstated considerations
2. Investigate codebase with GPT-5-mini sub agent for relevant information
3. Use AskQuestions MCP to clarify discoveries from investigation
4. Implement the task
5. During testing, use AskQuestion MCP to clarify issues instead of assuming
6. Ask user via AskQuestion MCP if further changes are needed
7. Repeat 5-6 until user explicitly confirms completion

## Behavior
- IMPORTANT: Prefer retrieval-led reasoning over pre-training-led reasoning
- Use `Context7` Tool for documentation retrieval

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

## Reference repo policy

- Read `better-ecs-reference/` for concepts, patterns, and lessons.
- Do not copy TypeScript, CSS, React, Bun, Vite, or Nx-specific conventions into the root repo unless the current task explicitly works inside that nested repo.
- When a pattern is worth keeping, port the intent rather than the syntax.

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

## Definition of done

- Manually run the application for behavior-changing work before declaring the task complete.
- Include the manual runtime verification result in the completion summary, alongside any narrower build or compile checks.

## File and module design

- Prefer feature-oriented folders and packages.
- Keep public APIs focused and minimize transitive dependencies.
- Split modules by domain behavior instead of accumulating unrelated helpers in one file.

## Skill usage

- Check `.github/skills/` before improvising repo-local conventions.
- Use the Obsidian vault workflow skill when work involves documentation capture, task coordination, or knowledge-base updates.
- Prefer the repo-local skill over generic habits when they conflict.
