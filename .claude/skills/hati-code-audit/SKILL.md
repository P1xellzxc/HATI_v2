---
name: hati-code-audit
description: Adversarially audit the HATI² web app code for money errors, data loss, security holes, broken navigation and accessibility semantics, with an executable reproduction for every finding. Use for the "audit" step of each iteration.
---

# HATI² code auditor

## Deeper version of this task

1. **What a top professional does that nobody asked for.** Attacks the change instead of reading it approvingly. Traces real paths a user or a malicious share code can take. Proves every claim with a reproduction, and throws away anything that doesn't reproduce. Ranks by harm to a real group splitting real money: wrong balances and lost data beat style.
2. **The rawest material.** `git diff` since the last audited commit, the full `web/src/app.src.html`, the raw `web/test.sh` output, stored `localStorage` JSON, share-code payloads, and `CalculateDebtsUseCase.kt` for parity. Not the coder's report of what changed.
3. **Methods and tools.** A threat model of share-code import (injection, oversized or malformed data, id collisions); fuzzing `parseMoney`, `parseWeight`, `splitEqual`, `splitWeighted` and the debt engine with random inputs against invariants (sums exact, balances zero-sum, Kotlin parity); Playwright repros for navigation, reload, back button, two tabs, offline; axe-core for semantics; grep for unescaped `${` interpolation.
4. **What shows the audit is right, other than the auditor or the user.** Each finding ships with a script that fails now; after the fix the same script passes; findings that don't reproduce are dropped.

## Rewritten prompt (this is how you operate)

Build and serve the app with `web/test.sh <dir>` (or build, then `python3 -m http.server --directory docs/app`). Audit the current code, prioritizing what changed since the last round.

For each finding give: severity (blocker = wrong money, data loss, security, app unusable; annoying = a task is confusing or fails sometimes; polish), where (function and line), a repro (Playwright or node snippet, inline), what the user sees, and a concrete fix. Run every repro; include only findings whose repro fails today. Also report what you checked that passed. Do not edit files under `web/src` or `docs/`.
