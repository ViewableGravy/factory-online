---
name: investigation
description: Investigate bugs, regressions, determinism failures, visual glitches, startup issues, crashes, or threading problems before attempting a fix.
---

# Investigation

## Purpose

Provide a strict workflow for debugging reported problems so fixes are based on objective reproduction instead of guesses.

## When to use

- A user reports incorrect behavior and wants the cause identified before or alongside a fix.
- A bug may be visual, timing-sensitive, deterministic, data-dependent, or hard to reproduce.
- A regression or testable failure should be locked down before implementation.

## Workflow

1. Reproduce the issue objectively before changing code.
   - Prefer one of these:
     - a failing automated test,
     - a deterministic reproduction harness,
     - a crash log, sanitizer report, or debugger trace,
     - a direct code-path reproduction that proves the incorrect state.
2. If the issue is not reproducible, stop and gather more information.
   - Explain what was attempted.
   - Explain what remains ambiguous.
   - Ask for the missing scenario details instead of guessing.
3. If the issue is a regression, or can reasonably be tested, add or update a test that reproduces it.
   - Verify that the test fails before implementing the fix.
4. Implement the fix with the smallest code change that resolves the verified cause.
   - Avoid unrelated cleanup.
5. Verify the fix objectively.
   - Re-run the reproduction.
   - Re-run the regression test.
   - Run the relevant build, test, sanitizer, or static-analysis command for the touched slice.
6. If the issue remains unresolved, treat the missing reproduction details as the next blocking requirement.

## Expectations

- Reproduction first, fix second.
- Evidence over intuition.
- Minimal change over speculative rewrite.
- If verification cannot be completed, say exactly what could not be verified and why.