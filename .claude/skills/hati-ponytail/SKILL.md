---
name: hati-ponytail
description: Run a ponytail quality review on every HATI² change (code and design diffs) and a ponytail audit of web/ before each ship — correctness, safety, load, missing tests, speed and what to cut, in plain English with fixes. Use after every Do and Revise step, especially code changes, and before shipping.
---

# HATI² ponytail review and audit

## Goal first

Use this skill for work that moves the project goal in `web/README.md`: **G1** works on Android and iOS, **G2** real group use with correct money and no data loss, **G3** not AI slop. A ponytail finding matters when it threatens G2 (wrong money, lost data, security) or G1 (breaks on a phone or offline), or when it is code that should not exist. Say which criterion each finding affects.

## Decide, don't wait

Decisions belong to the skill whose specialty they are (see "Decision owners" in `brain/Team log.md`). If it's yours, decide. If it's another skill's, route it there in the log.

The owner has delegated decisions inside the project to the team. When a choice comes up in your area, make it yourself against the goal (G1–G3), using the evidence you have; prefer the reversible, lowest-risk option that unblocks the goal; record it under Decisions in `brain/Team log.md` with the reason; and carry on. Ask the owner only for actions outside the repository that only they can take (for example enabling GitHub Pages, sharing a private link, accounts or payments).

## Before you start: deepen your assignment

Apply this prompt to the specific assignment you were just given (not to the skill in general), every time:

> "I want a deeper version of this task, not a bigger one. Before rewriting it: (1) say what a top professional in this field would do here that I'm not asking for; (2) name the rawest material they would start from instead of what has already been digested for me; (3) name the specific methods and tools they would use; (4) say what, other than you or me, would show the result is right. Then rewrite my prompt to ask for exactly that."

Write your four answers and the rewritten assignment (a few lines each) at the top of your entry in `brain/Team log.md`, then do the rewritten assignment. Deeper means more rigorous within your role and the goal, never more scope.

## Deeper version of this task

1. **What a top professional does that nobody asked for.** Reviews a change like the developer who will be paged when it breaks: reads the callers of every changed function, not just the diff; asks what the change can break that it doesn't touch; prefers fixes that delete code; and asks for one good test where risky logic has none.
2. **The rawest material.** The actual diff (`git diff HEAD` for uncommitted work, or `git diff <base>..HEAD`), the full functions it touches in `web/src/app.src.html`, `web/tests/e2e.js`, and `web/README.md` invariants — not the doer's summary.
3. **Methods and tools.** The installed ponytail skills: `anthropic-skills:ponytail-review` for a change and `anthropic-skills:ponytail-audit` for the whole `web/` folder (or read them from `/root/.claude/skills/synced/*/ponytail-review/SKILL.md` and `.../ponytail-audit/SKILL.md`). Grep every caller of a changed function; run `web/test.sh <dir>`; write a short repro for each claimed bug.
4. **What shows the result is right, other than you or the user.** Every finding has a concrete failing case or repro; "Must fix" items are fixed and their repro passes; the lean count (lines that can go) goes down over rounds.

## Rewritten prompt (this is how you operate)

Scope and load for HATI²: the app is `web/src/app.src.html` (built to `docs/app/`). Expected load: one person per phone, a few hundred to a few thousand expenses per volume, offline-first, data in `localStorage`, share codes from other people are untrusted input. Money paths, import (`validVolume`, `cleanVolume`), storage (`load`, `normalize`, `save`), navigation and the service worker are where mistakes cost most.

- **After a change (Do or Revise step):** invoke `anthropic-skills:ponytail-review` on the uncommitted diff (`git diff HEAD -- web/ docs/app/sw.js`). Follow it fully: understand first, look for bug, risk, scale, missing test, speed, lean; check before you report.
- **Before shipping:** invoke `anthropic-skills:ponytail-audit` on `web/`.

Report in the ponytail output format (numbered Must fix / Should fix / Nice to have, each with What this is / Problem / Fix / If we skip it, then Verdict, Lean, Not checked). Add for each finding which of G1–G3 it threatens. Do not edit code yourself unless your instructions say you are fixing; then fix Must-fix items first, add a test for each, and run `web/test.sh`.
