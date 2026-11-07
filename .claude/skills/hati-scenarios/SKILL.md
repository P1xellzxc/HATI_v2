---
name: hati-scenarios
description: Run and extend the library of real-use HATI² scenarios (whole jobs like splitting a dinner or sharing a trip between two phones) as end-to-end checks, measuring taps, errors and dead ends. Use when planning, coding acceptance checks, or auditing usability.
---

# HATI² real-use scenarios

## Goal first

Use this skill only for work that moves the project goal in `web/README.md`: **G1** works on Android and iOS from a link (offline included), **G2** real group use with correct money and no data loss, **G3** not AI slop. Say which criterion your use of this skill serves. This is G2, tested the way users actually work.

## Decide, don't wait

Decisions belong to the skill whose specialty they are (see "Decision owners" in `brain/Team log.md`). If it's yours, decide. If it's another skill's, route it there in the log.

The owner has delegated decisions inside the project to the team. When a choice comes up in your area, make it yourself against the goal (G1–G3), using the evidence you have; prefer the reversible, lowest-risk option that unblocks the goal; record it under Decisions in `brain/Team log.md` with the reason; and carry on. Ask the owner only for actions outside the repository that only they can take (for example enabling GitHub Pages, sharing a private link, accounts or payments).

## Before you start: deepen your assignment

Apply this prompt to the specific assignment you were just given (not to the skill in general), every time:

> "I want a deeper version of this task, not a bigger one. Before rewriting it: (1) say what a top professional in this field would do here that I'm not asking for; (2) name the rawest material they would start from instead of what has already been digested for me; (3) name the specific methods and tools they would use; (4) say what, other than you or me, would show the result is right. Then rewrite my prompt to ask for exactly that."

Write your four answers and the rewritten assignment (a few lines each) at the top of your entry in `brain/Team log.md`, then do the rewritten assignment. Deeper means more rigorous within your role and the goal, never more scope.

## Deeper version of this task
1. **Top professional:** tests whole jobs, not screens: the money must come out right and the person must get there without help. Records where people would hesitate, not just whether it is possible.
2. **Rawest material:** the scenario library in `web/tests/scenarios.md`, the running app, and the stored data after each step.
3. **Methods/tools:** Playwright scripts that act out each scenario on an iPhone-sized viewport; count taps and wrong turns; assert the final balances to the cent; add passing scenarios to `web/tests/e2e.js`.
4. **What shows it's right:** each scenario's final balances match a hand calculation written in the library; tap counts stay within the stated budget; the check fails if a step breaks.


## Rewritten prompt (this is how you operate)

For your assignment, run each relevant scenario in `web/tests/scenarios.md` as a Playwright script on an iPhone-sized viewport. Assert the final money to the cent against the hand calculation, count taps against the budget, and note every step that needs guesswork. Report failures and over-budget scenarios as findings with the script; when you are coding, add the scenario to `web/tests/e2e.js` as `scenario: <title>`.

## How to use
- Planner: pick scenarios that fail or are over their tap budget and turn them into plan items.
- Coder: implement a scenario as an e2e check named `scenario: <name>`.
- Auditors: run the library each round; report scenarios that fail, exceed their tap budget, or need guesswork.
- Add new scenarios to `web/tests/scenarios.md` when you find a real job the library misses.

