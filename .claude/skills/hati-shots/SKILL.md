---
name: hati-shots
description: Take the standard HATI² screenshot set (every view, sheet and key state at 360/390 px, light/dark, three palettes) with one command, so every role judges the same evidence. Use before any design decision, design audit, plan, or before/after comparison.
---

# HATI² screenshot kit

## Goal first

Use this skill only for work that moves the project goal in `web/README.md`: **G1** works on Android and iOS from a link (offline included), **G2** real group use with correct money and no data loss, **G3** not AI slop. Say which criterion your use of this skill serves. Mostly G3 and G1 (phone fit), as evidence.

## Decide, don't wait

Decisions belong to the skill whose specialty they are (see "Decision owners" in `brain/Team log.md`). If it's yours, decide. If it's another skill's, route it there in the log.

The owner has delegated decisions inside the project to the team. When a choice comes up in your area, make it yourself against the goal (G1–G3), using the evidence you have; prefer the reversible, lowest-risk option that unblocks the goal; record it under Decisions in `brain/Team log.md` with the reason; and carry on. Ask the owner only for actions outside the repository that only they can take (for example enabling GitHub Pages, sharing a private link, accounts or payments).

## Before you start: deepen your assignment

Apply this prompt to the specific assignment you were just given (not to the skill in general), every time:

> "I want a deeper version of this task, not a bigger one. Before rewriting it: (1) say what a top professional in this field would do here that I'm not asking for; (2) name the rawest material they would start from instead of what has already been digested for me; (3) name the specific methods and tools they would use; (4) say what, other than you or me, would show the result is right. Then rewrite my prompt to ask for exactly that."

Write your four answers and the rewritten assignment (a few lines each) at the top of your entry in `brain/Team log.md`, then do the rewritten assignment. Deeper means more rigorous within your role and the goal, never more scope.

## Deeper version of this task
1. **Top professional:** never judges a UI from memory or from one hand-picked screen; uses a fixed, reproducible capture set so rounds can be compared and nothing is cherry-picked.
2. **Rawest material:** the rendered app from the current source, rebuilt now, not last round's images or someone's description.
3. **Methods/tools:** `web/shots.sh <dir>` (build, serve, capture) driving `web/tests/shots.js`; contact sheet `<dir>/index.html`; `--contrast` adds measured WCAG ratios via `web/tests/contrast.js`.
4. **What shows it's right:** the same file names every round (`<palette>-<mode>-<width>-<view>.png`), page errors printed, and a contact sheet anyone can open.


## Rewritten prompt (this is how you operate)

Before judging anything visual, run `PORT=<free port> web/shots.sh <your-dir> [--contrast]`, open the contact sheet, look at every image that matters to your assignment, and cite file paths in your findings. If the kit lacks a view or state you need, add it to `web/tests/shots.js` for everyone instead of writing a private script.

## How to use
- Run `PORT=<free port> web/shots.sh <your-dir> [--contrast]`. Pick a port no other role uses this round.
- Look at the images (Read tool) before writing conclusions; cite file paths.
- Need a view the kit lacks? Add it to `VIEWS` in `web/tests/shots.js` so everyone gets it from then on; don't write a private screenshot script.

