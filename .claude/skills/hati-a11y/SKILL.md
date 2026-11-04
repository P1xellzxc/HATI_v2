---
name: hati-a11y
description: Check and fix HATI² accessibility (keyboard, screen reader semantics, focus, contrast in all 13 palettes and both themes, zoom, reduced motion) with measured results. Use when auditing or changing UI structure, colors or interaction.
---

# HATI² accessibility

## Goal first

Use this skill only for work that moves the project goal in `web/README.md`: **G1** works on Android and iOS from a link (offline included), **G2** real group use with correct money and no data loss, **G3** not AI slop. Say which criterion your use of this skill serves. G1 and G2 for everyone, including screen reader, keyboard and low-vision users.

## Decide, don't wait

Decisions belong to the skill whose specialty they are (see "Decision owners" in `.claude/team-log.md`). If it's yours, decide. If it's another skill's, route it there in the log.

The owner has delegated decisions inside the project to the team. When a choice comes up in your area, make it yourself against the goal (G1–G3), using the evidence you have; prefer the reversible, lowest-risk option that unblocks the goal; record it under Decisions in `.claude/team-log.md` with the reason; and carry on. Ask the owner only for actions outside the repository that only they can take (for example enabling GitHub Pages, sharing a private link, accounts or payments).

## Before you start: deepen your assignment

Apply this prompt to the specific assignment you were just given (not to the skill in general), every time:

> "I want a deeper version of this task, not a bigger one. Before rewriting it: (1) say what a top professional in this field would do here that I'm not asking for; (2) name the rawest material they would start from instead of what has already been digested for me; (3) name the specific methods and tools they would use; (4) say what, other than you or me, would show the result is right. Then rewrite my prompt to ask for exactly that."

Write your four answers and the rewritten assignment (a few lines each) at the top of your entry in `.claude/team-log.md`, then do the rewritten assignment. Deeper means more rigorous within your role and the goal, never more scope.

## Deeper version of this task
1. **Top professional:** measures instead of eyeballing, tests with the keyboard alone, and checks every palette and theme because one bad palette is a broken app for whoever picks it.
2. **Rawest material:** the rendered DOM and computed styles, axe-core results, and `web/tests/contrast.js` output across all palettes.
3. **Methods/tools:** `web/shots.sh <dir> --contrast` (all 13 palettes × light/dark × 4 views); axe-core (`npm i axe-core` in a scratch folder, inject `axe.min.js` with Playwright, run on every view and an open sheet); keyboard-only walk-through (Tab order, Escape closes sheets, focus returns); 200% zoom at 390 px; `prefers-reduced-motion`.
4. **What shows it's right:** contrast.js passes (all text ≥ 4.5:1); axe reports no serious or critical violations; keyboard walk-through completes every core task.


## Rewritten prompt (this is how you operate)

For your assignment, run `web/shots.sh <dir> --contrast`, run axe-core on every view and an open sheet, and do a keyboard-only walk-through of the core tasks. Report each violation with the element, the measured value or axe rule, its severity by goal impact, and a fix.

## Known traps in this app
- Palette previews in Settings show other themes on purpose; contrast.js skips them.
- `#app` must not be a live region; announcements go through `#status`.
- Sheets are `role=dialog` with `inert` on the page behind; focus moves to the sheet title and returns on close.

