---
name: hati-shots
description: Take the standard HATI² screenshot set (every view, sheet and key state at 360/390 px, light/dark, three palettes) with one command, so every role judges the same evidence. Use before any design decision, design audit, plan, or before/after comparison.
---

# HATI² screenshot kit

## Goal first

Use this skill only for work that moves the project goal in `web/README.md`: **G1** works on Android and iOS from a link (offline included), **G2** real group use with correct money and no data loss, **G3** not AI slop. Say which criterion your use of this skill serves. Mostly G3 and G1 (phone fit), as evidence.

## Deeper version of this task
1. **Top professional:** never judges a UI from memory or from one hand-picked screen; uses a fixed, reproducible capture set so rounds can be compared and nothing is cherry-picked.
2. **Rawest material:** the rendered app from the current source, rebuilt now, not last round's images or someone's description.
3. **Methods/tools:** `web/shots.sh <dir>` (build, serve, capture) driving `web/tests/shots.js`; contact sheet `<dir>/index.html`; `--contrast` adds measured WCAG ratios via `web/tests/contrast.js`.
4. **What shows it's right:** the same file names every round (`<palette>-<mode>-<width>-<view>.png`), page errors printed, and a contact sheet anyone can open.

## How to use
- Run `PORT=<free port> web/shots.sh <your-dir> [--contrast]`. Pick a port no other role uses this round.
- Look at the images (Read tool) before writing conclusions; cite file paths.
- Need a view the kit lacks? Add it to `VIEWS` in `web/tests/shots.js` so everyone gets it from then on; don't write a private screenshot script.

