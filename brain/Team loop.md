---
title: "Team loop"
tags: ["hati/process"]
---

Plan → do → audit → revise rounds on the web app until the [[Goal]] is met (max 3 per run).

1. **Plan:** [[hati-plan]] picks at most 5 items, each with a check that proves it; a critic (code + design view) amends.
2. **Do:** [[hati-design]], then [[hati-code]] (same file, so in sequence).
3. **Ponytail:** [[hati-ponytail]] reviews the change.
4. **Audit, in parallel:** [[hati-code-audit]], [[hati-design-audit]], iOS tester ([[hati-ios]]), scenario tester ([[hati-scenarios]]), [[hati-pixel-perfectionist]].
5. **Revise:** designer and coder fix serious findings; the finding's auditor re-checks.
6. **Ponytail again:** review of the round's diff plus an audit of `web/`; must-fix items get fixed.
7. **Ship:** [[hati-ship]] tests, commits, pushes.
8. **Judge:** [[hati-goal-judge]] decides from its own evidence whether the goal is reached.

Every agent first applies the "deeper version" prompt to its own assignment, writes a handoff in the [[Team log]], decides what falls in its specialty ([[Decisions]]), and runs the [[hati-lazy-check]] in design work.

Back to [[HATI Brain]]
