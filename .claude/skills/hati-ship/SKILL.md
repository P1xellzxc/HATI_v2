---
name: hati-ship
description: Ship a HATI² round safely: run the tests, rebuild deterministically, make sure installed copies update, commit with the house format, push with retries, and republish the preview. Use for the ship step of each round.
---

# HATI² ship

## Goal first

Use this skill only for work that moves the project goal in `web/README.md`: **G1** works on Android and iOS from a link (offline included), **G2** real group use with correct money and no data loss, **G3** not AI slop. Say which criterion your use of this skill serves. G1: what ships must reach installed phones; G2: nothing broken ships.

## Decide, don't wait

Decisions belong to the skill whose specialty they are (see "Decision owners" in `.claude/team-log.md`). If it's yours, decide. If it's another skill's, route it there in the log.

The owner has delegated decisions inside the project to the team. When a choice comes up in your area, make it yourself against the goal (G1–G3), using the evidence you have; prefer the reversible, lowest-risk option that unblocks the goal; record it under Decisions in `.claude/team-log.md` with the reason; and carry on. Ask the owner only for actions outside the repository that only they can take (for example enabling GitHub Pages, sharing a private link, accounts or payments).

## Before you start: deepen your assignment

Apply this prompt to the specific assignment you were just given (not to the skill in general), every time:

> "I want a deeper version of this task, not a bigger one. Before rewriting it: (1) say what a top professional in this field would do here that I'm not asking for; (2) name the rawest material they would start from instead of what has already been digested for me; (3) name the specific methods and tools they would use; (4) say what, other than you or me, would show the result is right. Then rewrite my prompt to ask for exactly that."

Write your four answers and the rewritten assignment (a few lines each) at the top of your entry in `.claude/team-log.md`, then do the rewritten assignment. Deeper means more rigorous within your role and the goal, never more scope.

## Deeper version of this task
1. **Top professional:** never ships red; checks that the build is reproducible and that existing installs will actually update; writes commit messages that explain why.
2. **Rawest material:** `git status`/`git diff`, the raw `web/test.sh` output, `docs/app/sw.js` cache name, and the remote branch state.
3. **Methods/tools:** `web/test.sh <dir>`; `python3 -I web/tools/build.py` twice → no diff (deterministic); confirm the `CACHE` name in `docs/app/sw.js` changed when shipped files changed (the build hashes content); `git push` with 2/4/8/16 s backoff and a 90 s timeout per attempt.
4. **What shows it's right:** tests green, second build shows no diff, cache name changed iff app files changed, push confirmed by `git status -sb` showing no ahead/behind.


## Rewritten prompt (this is how you operate)

Follow the checklist below in order and stop at the first failure, reporting it instead of shipping.

## Checklist
1. `web/test.sh <dir>` — no FAIL lines.
2. Rebuild; `git status` shows no further change.
3. Stage only project files (`web/`, `docs/`, `.claude/`, `.gitignore`); never stage throwaway scripts or screenshots.
4. Commit: first line summarizes user-visible changes; body lists them; end with the two attribution lines the orchestrator gives you.
5. Push with retries; verify.

