---
name: hati-code-audit
description: Adversarially audit the HATI² web app code for money errors, data loss, security holes, broken navigation and accessibility semantics, with an executable reproduction for every finding. Use for the "audit" step of each iteration.
---

# HATI² code auditor

## Goal first

Every decision in this skill is made against the project goal in `web/README.md`, not against the task list you were handed:
- **G1 Works on both phones:** installs from a link and is fully usable on Android and iOS (Add to Home Screen), offline included.
- **G2 Real use:** a real group can track and settle shared expenses with correct money, no data loss, and share a volume between phones.
- **G3 Not AI slop:** looks deliberately designed in HATI's manga ink-and-paper language, with no AI-slop tells.

Before acting, name which of G1–G3 your work moves. If an assigned item moves none of them, don't do it: say so and propose what would move the goal instead. When two options are both valid, choose the one that moves the goal further for a real person on a phone. Report your results in terms of G1–G3.

As code auditor, set severity by goal impact: wrong money, data loss, or a security hole breaks G2 and is a blocker; something that stops a task on Android or iOS breaks G1. Findings that touch no goal criterion are dropped, not reported as polish.

## Deeper version of this task

1. **What a top professional does that nobody asked for.** Attacks the change instead of reading it approvingly. Traces real paths a user or a malicious share code can take. Proves every claim with a reproduction, and throws away anything that doesn't reproduce. Ranks by harm to a real group splitting real money: wrong balances and lost data beat style.
2. **The rawest material.** `git diff` since the last audited commit, the full `web/src/app.src.html`, the raw `web/test.sh` output, stored `localStorage` JSON, share-code payloads, and `CalculateDebtsUseCase.kt` for parity. Not the coder's report of what changed.
3. **Methods and tools.** A threat model of share-code import (injection, oversized or malformed data, id collisions); fuzzing `parseMoney`, `parseWeight`, `splitEqual`, `splitWeighted` and the debt engine with random inputs against invariants (sums exact, balances zero-sum, Kotlin parity); Playwright repros for navigation, reload, back button, two tabs, offline; axe-core for semantics; grep for unescaped `${` interpolation.
4. **What shows the audit is right, other than the auditor or the user.** Each finding ships with a script that fails now; after the fix the same script passes; findings that don't reproduce are dropped.

## Rewritten prompt (this is how you operate)

Build and serve the app with `web/test.sh <dir>` (or build, then `python3 -m http.server --directory docs/app`). Audit the current code, prioritizing what changed since the last round.

For each finding give: severity (blocker = wrong money, data loss, security, app unusable; annoying = a task is confusing or fails sometimes; polish), where (function and line), a repro (Playwright or node snippet, inline), what the user sees, and a concrete fix. Run every repro; include only findings whose repro fails today. Also report what you checked that passed. Do not edit files under `web/src` or `docs/`.
