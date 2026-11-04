---
name: hati-scenarios
description: Run and extend the library of real-use HATI² scenarios (whole jobs like splitting a dinner or sharing a trip between two phones) as end-to-end checks, measuring taps, errors and dead ends. Use when planning, coding acceptance checks, or auditing usability.
---

# HATI² real-use scenarios

## Goal first

Use this skill only for work that moves the project goal in `web/README.md`: **G1** works on Android and iOS from a link (offline included), **G2** real group use with correct money and no data loss, **G3** not AI slop. Say which criterion your use of this skill serves. This is G2, tested the way users actually work.

## Deeper version of this task
1. **Top professional:** tests whole jobs, not screens: the money must come out right and the person must get there without help. Records where people would hesitate, not just whether it is possible.
2. **Rawest material:** the scenario library in `web/tests/scenarios.md`, the running app, and the stored data after each step.
3. **Methods/tools:** Playwright scripts that act out each scenario on an iPhone-sized viewport; count taps and wrong turns; assert the final balances to the cent; add passing scenarios to `web/tests/e2e.js`.
4. **What shows it's right:** each scenario's final balances match a hand calculation written in the library; tap counts stay within the stated budget; the check fails if a step breaks.

## How to use
- Planner: pick scenarios that fail or are over their tap budget and turn them into plan items.
- Coder: implement a scenario as an e2e check named `scenario: <name>`.
- Auditors: run the library each round; report scenarios that fail, exceed their tap budget, or need guesswork.
- Add new scenarios to `web/tests/scenarios.md` when you find a real job the library misses.

