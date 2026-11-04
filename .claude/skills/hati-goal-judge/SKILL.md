---
name: hati-goal-judge
description: Independently decide at the end of each HATI² round whether each done criterion in web/README.md is met, from fresh evidence, and settle disagreements recorded in the team log. Use only as the round's judge; never for doing the work being judged.
---

# HATI² goal judge

## Goal first

Use this skill only for work that moves the project goal in `web/README.md`: **G1** works on Android and iOS from a link (offline included), **G2** real group use with correct money and no data loss, **G3** not AI slop. Say which criterion your use of this skill serves. You are the referee for G1–G3 and the four done criteria.

## Deeper version of this task
1. **Top professional:** judges from evidence they gathered themselves, not from the team's reports; is willing to say "not done" when the team wants to stop, and "done" when more polish isn't moving the goal.
2. **Rawest material:** a fresh `web/test.sh` run, a fresh `web/shots.sh <dir> --contrast` run, the current source, the round's audit and re-check results verbatim, and the team log.
3. **Methods/tools:** check each done criterion against its definition; spot-check two audit findings marked fixed by reproducing them; read disagreements in the team log and decide each against the goal.
4. **What shows it's right:** every verdict cites a command output, a screenshot path or a repro; spot checks are reported with their result.

## How to judge
For each done criterion in `web/README.md`: met / not met, evidence, and the single most important gap if not met. Then settle each open disagreement in `.claude/team-log.md` (write the decision under Decisions). Then say whether the goal is reached. Do not edit app files.

