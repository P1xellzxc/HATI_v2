---
name: hati-lazy-check
description: Run the lazy-but-meticulous user check on the HATI² web app (understood in 3 seconds, goal in one tap, nothing unfinished) across first screen, one-tap paths, phone fit, states and copy. Use in every design step, every design audit, and when planning against the done criteria.
---

# HATI² lazy-user check

## Goal first

Use this skill only for work that moves the project goal in `web/README.md`: **G1** works on Android and iOS from a link (offline included), **G2** real group use with correct money and no data loss, **G3** not AI slop. Say which criterion your use of this skill serves. Done criterion 4 in `web/README.md`; serves G1–G3.

## Decide, don't wait

Decisions belong to the skill whose specialty they are (see "Decision owners" in `.claude/team-log.md`). If it's yours, decide. If it's another skill's, route it there in the log.

The owner has delegated decisions inside the project to the team. When a choice comes up in your area, make it yourself against the goal (G1–G3), using the evidence you have; prefer the reversible, lowest-risk option that unblocks the goal; record it under Decisions in `.claude/team-log.md` with the reason; and carry on. Ask the owner only for actions outside the repository that only they can take (for example enabling GitHub Pages, sharing a private link, accounts or payments).

## Before you start: deepen your assignment

Apply this prompt to the specific assignment you were just given (not to the skill in general), every time:

> "I want a deeper version of this task, not a bigger one. Before rewriting it: (1) say what a top professional in this field would do here that I'm not asking for; (2) name the rawest material they would start from instead of what has already been digested for me; (3) name the specific methods and tools they would use; (4) say what, other than you or me, would show the result is right. Then rewrite my prompt to ask for exactly that."

Write your four answers and the rewritten assignment (a few lines each) at the top of your entry in `.claude/team-log.md`, then do the rewritten assignment. Deeper means more rigorous within your role and the goal, never more scope.

## Deeper version of this task
1. **Top professional:** tests like an impatient stranger, not like the person who built it; notices what's unfinished before what's clever.
2. **Rawest material:** fresh screenshots from `hati-shots` (360 and 390 px, light and dark), plus a live click-through on an iPhone-sized viewport.
3. **Methods/tools:** a 3-second first-screen read; counting taps to each core goal; tap-target and overflow measurement with bounding boxes; state screenshots (empty, one member, long text, sheet open, error).
4. **What shows it's right:** pass/fail per point with screenshot paths and measured numbers.

**Persona:** gives the app 10 seconds and zero effort; won't read, scroll far, hunt for buttons or forgive a blank screen; spots 2px misalignment, wrong cursor, internal names, flashes of empty content, dead controls.

**Pass condition:** understood within 3 seconds, goal in one tap, nothing looks unfinished.


## Rewritten prompt (this is how you operate)

For your assignment, take fresh screenshots (use `hati-shots`) and walk the five points below as the persona. Report pass or fail per point with screenshot paths and measured numbers; when you are designing, run it on your before and after screenshots.

## The five points
1. **First screen:** what is it, who is it for, what do I do next; main action visible; nothing empty or flashing.
2. **One-tap paths:** add expense, see who owes whom, settle up, share, change theme — each one tap from where a user looks, with a way back.
3. **Phone fit:** no sideways scroll, targets ≥ 44 px, nothing cut off, readable text, inputs ≥ 16 px.
4. **States:** empty, one item, long names and amounts, sheet open, error — designed, not broken.
5. **Copy:** no internal wording, correct plurals, money and dates for people (see `hati-copy`).

Any failure is at least annoying; a confusing first screen or a blank/broken state is a blocker.

