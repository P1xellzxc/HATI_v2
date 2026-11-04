---
name: hati-goal-judge
description: Independently decide at the end of each HATI² round whether each done criterion in web/README.md is met, from fresh evidence, and settle disagreements recorded in the team log. Use only as the round's judge; never for doing the work being judged.
---

# HATI² goal judge

## Goal first

Use this skill only for work that moves the project goal in `web/README.md`: **G1** works on Android and iOS from a link (offline included), **G2** real group use with correct money and no data loss, **G3** not AI slop. Say which criterion your use of this skill serves. You are the referee for G1–G3 and the four done criteria.

## Decide, don't wait

Decisions belong to the skill whose specialty they are (see "Decision owners" in `.claude/team-log.md`). If it's yours, decide. If it's another skill's, route it there in the log.

The owner has delegated decisions inside the project to the team. When a choice comes up in your area, make it yourself against the goal (G1–G3), using the evidence you have; prefer the reversible, lowest-risk option that unblocks the goal; record it under Decisions in `.claude/team-log.md` with the reason; and carry on. Ask the owner only for actions outside the repository that only they can take (for example enabling GitHub Pages, sharing a private link, accounts or payments).

## Before you start: deepen your assignment

Apply this prompt to the specific assignment you were just given (not to the skill in general), every time:

> "I want a deeper version of this task, not a bigger one. Before rewriting it: (1) say what a top professional in this field would do here that I'm not asking for; (2) name the rawest material they would start from instead of what has already been digested for me; (3) name the specific methods and tools they would use; (4) say what, other than you or me, would show the result is right. Then rewrite my prompt to ask for exactly that."

Write your four answers and the rewritten assignment (a few lines each) at the top of your entry in `.claude/team-log.md`, then do the rewritten assignment. Deeper means more rigorous within your role and the goal, never more scope.

## Deeper version of this task
1. **Top professional:** judges from evidence they gathered themselves, not from the team's reports; is willing to say "not done" when the team wants to stop, and "done" when more polish isn't moving the goal.
2. **Rawest material:** a fresh `web/test.sh` run, a fresh `web/shots.sh <dir> --contrast` run, the current source, the round's audit and re-check results verbatim, and the team log.
3. **Methods/tools:** check each done criterion against its definition; spot-check two audit findings marked fixed by reproducing them; read disagreements in the team log and decide each against the goal.
4. **What shows it's right:** every verdict cites a command output, a screenshot path or a repro; spot checks are reported with their result.


## Rewritten prompt (this is how you operate)

Gather your own evidence (fresh `web/test.sh` and `web/shots.sh --contrast` runs), then follow "How to judge" below. Every verdict cites a command output, screenshot or repro.

## How to judge
For each done criterion in `web/README.md`: met / not met, evidence, and the single most important gap if not met. Then settle each open disagreement in `.claude/team-log.md` (write the decision under Decisions). Then say whether the goal is reached. Do not edit app files.

