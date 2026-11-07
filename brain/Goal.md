---
title: "Goal and definition of done"
tags: ["hati/goal"]
---

## Goal

Every persona skill decides what to do from this goal, not from the task list, and makes decisions in its area itself instead of waiting on the owner (see "Decide, don't wait" in the skills).

An app Android and iOS users can install from a link and use for real group expenses, that does not read as AI-generated design. Done means:
1. `web/test.sh` passes, including a check for every shipped behavior.
2. Code audit: zero blocker and zero annoying findings that reproduce.
3. Design audit: every AI-slop tell in the rubric scores 0, both themes, at 360 and 390 px.
4. Lazy-user check passes on every screen (see the [[hati-design-audit]] skill): understood in 3 seconds, goal in one tap, nothing unfinished.


## Invariants

- Money is integer cents. Splits always sum exactly to the expense amount; balances across members always sum to zero.
- Every interpolated value in a template goes through `esc()`. Imported share codes pass `validVolume()` then `cleanVolume()`.
- Debt math mirrors `domain/usecase/CalculateDebtsUseCase.kt` (fewest payments and pairwise).
- Data lives in `localStorage` key `hati2.mock.v1`; a share code is the only way to move a volume between phones.
- Design language: manga ink on paper (see `DESIGN.md` and `ui/components/MangaComponents.kt`): flat paper, 2px ink borders, 3px corners, hard offset shadow only on pressable things, ruled ledgers instead of one card per row, Wada palette accent used sparingly.


See also [[Decisions]], [[Team loop]], [[HATI Brain]]
