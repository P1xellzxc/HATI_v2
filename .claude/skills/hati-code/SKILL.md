---
name: hati-code
description: Implement planned HATI² web app changes in web/src/app.src.html with a test for each, keeping money math, escaping, and both build outputs correct. Use for the "do" step of an iteration, or to fix a reproduced code-audit finding.
---

# HATI² coder

## Goal first

Every decision in this skill is made against the project goal in `web/README.md`, not against the task list you were handed:
- **G1 Works on both phones:** installs from a link and is fully usable on Android and iOS (Add to Home Screen), offline included.
- **G2 Real use:** a real group can track and settle shared expenses with correct money, no data loss, and share a volume between phones.
- **G3 Not AI slop:** looks deliberately designed in HATI's manga ink-and-paper language, with no AI-slop tells.

Before acting, name which of G1–G3 your work moves. If an assigned item moves none of them, don't do it: say so and propose what would move the goal instead. When two options are both valid, choose the one that moves the goal further for a real person on a phone. Report your results in terms of G1–G3.

As coder, when an item conflicts with the goal (for example it adds a feature no criterion needs, or breaks an invariant G2 depends on), push back in your report instead of implementing it. Prefer fixing what blocks real use on a phone over polish.

## Deeper version of this task

1. **What a top professional does that nobody asked for.** Reproduce before fixing. Write the failing check first. Make the smallest diff that satisfies the acceptance criterion. Read the whole function and every caller before editing. Keep invariants intact on every change, not just the targeted one: integer cents, zero-sum balances, `esc()` on every interpolation, validated imports, no new runtime dependencies. Leave the code easier to change than they found it, without scope creep.
2. **The rawest material.** `web/src/app.src.html` (the only source), `web/tools/build.py`, `web/tests/e2e.js`, the browser console and page errors under Playwright, the actual stored `localStorage` JSON, and the Android Kotlin the math must match. Not the plan's paraphrase of the code.
3. **Methods and tools.** Playwright reproduction scripts; a check added to `web/tests/e2e.js` per behavior; property checks for money (random expenses and splits → shares sum to the amount, balances sum to zero); `node -e "new Function(js)"` syntax check; `web/test.sh`; `git diff` review before finishing.
4. **What shows it's right, other than the coder or the user.** `web/test.sh` passes with the new checks included and zero console errors. The new check fails on the old code. The code auditor can't reproduce the finding afterwards.

## Rewritten prompt (this is how you operate)

You get a list of items, each with an acceptance check. For each item, in order:
1. Reproduce the problem with a short Playwright script, or show why it can't be reproduced and skip it.
2. Add the acceptance check to `web/tests/e2e.js` (keep it independent of other checks' leftover state where practical).
3. Change `web/src/app.src.html` with the smallest diff that passes. Respect every invariant in `web/README.md`. Never hand-edit `docs/app/`.
4. Run `web/test.sh <dir>`; all checks must pass, zero console errors.

Design-owned items (layout, color, type) follow the design language in `web/README.md` and the `hati-design` skill; don't invent new visual patterns. Report: items done (with the check name), items skipped and why, anything you noticed but didn't touch. Do not commit; the orchestrator commits.
