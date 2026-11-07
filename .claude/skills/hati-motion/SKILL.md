---
name: hati-motion
description: Design, implement and audit motion in the HATI² web app — durations, easing, origins, staggers, press feedback, popovers, list and state transitions — using 20 concrete rules, with measured timings. Use whenever a change adds or touches animation, and in every design audit.
---

# HATI² motion

## Goal first

Use this skill for work that moves the project goal in `web/README.md`: **G1** works on Android and iOS, **G2** real group use, **G3** not AI slop. Motion that is slow, bouncy or floaty reads as generated (G3) and makes a money app feel unreliable (G2); motion that is fast and physical makes taps feel answered on a phone (G1). Say which criterion each change serves.

## Decide, don't wait

Decisions belong to the skill whose specialty they are (see "Decision owners" in `brain/Team log.md`). If it's yours, decide. If it's another skill's, route it there in the log.

The owner has delegated decisions inside the project to the team. When a choice comes up in your area, make it yourself against the goal (G1–G3), using the evidence you have; prefer the reversible, lowest-risk option that unblocks the goal; record it under Decisions in `brain/Team log.md` with the reason; and carry on. Ask the owner only for actions outside the repository that only they can take (for example enabling GitHub Pages, sharing a private link, accounts or payments).

## Before you start: deepen your assignment

Apply this prompt to the specific assignment you were just given (not to the skill in general), every time:

> "I want a deeper version of this task, not a bigger one. Before rewriting it: (1) say what a top professional in this field would do here that I'm not asking for; (2) name the rawest material they would start from instead of what has already been digested for me; (3) name the specific methods and tools they would use; (4) say what, other than you or me, would show the result is right. Then rewrite my prompt to ask for exactly that."

Write your four answers and the rewritten assignment (a few lines each) at the top of your entry in `brain/Team log.md`, then do the rewritten assignment. Deeper means more rigorous within your role and the goal, never more scope.

## Deeper version of this task

1. **What a top professional does that nobody asked for.** A motion designer treats every animation as information: it shows where something came from, where it went, and that the tap worked. They keep everyday UI fast and unbouncy, reserve one expressive moment per flow, measure durations instead of eyeballing, and always honour reduced motion.
2. **The rawest material.** The CSS transitions/animations and any JS timing in `web/src/app.src.html`, recorded at 60 fps (Playwright video or frame captures) on an iPhone-sized viewport, not a description of how it "feels".
3. **Methods and tools.** The 20 rules below as a checklist; `getComputedStyle(el).transitionDuration/TimingFunction` and `getAnimations()` to read real values; Playwright `recordVideo` or frame-by-frame screenshots to check origin, overlap and stagger; `prefers-reduced-motion` emulation.
4. **What shows the result is right, other than you or the user.** Measured durations and easings within the ranges below, origins that match the trigger element, no animation at all under reduced motion, and the lazy-user check still passing.

## Rewritten prompt (this is how you operate)

For your assignment, list every animation it adds or touches, measure its duration, easing, origin and stagger, and check each against the rules. Fix what breaks a rule with the smallest CSS change, using shared motion tokens (`--dur-in`, `--dur-out`, `--ease-out`, `--ease-in-out`, `--stagger`) instead of one-off numbers. Report each animation with its before/after values and the rule number.

## The 20 rules (source: "20 Ways to Make Beautiful AI Animations")

| # | Rule | HATI default |
|---|---|---|
| 1 | Ease-out for entrances | `--ease-out: cubic-bezier(.2,.8,.2,1)` |
| 2 | Dropdowns and sheets at 150–250 ms | `--dur-in: 200ms` |
| 3 | Stagger list rows by ~30 ms (not 200 ms) | `--stagger: 30ms`, max ~8 rows |
| 4 | Scale to 0.97 on press | clay controls: translate 2–3px + scale .97 |
| 5 | Stretch moving knobs ~12% (area-preserving) | segmented control / toggles |
| 6 | Stagger details as they settle (badge trails ~50 ms) | balance pill after the hero |
| 7 | Arcs, not straight lines, for objects that travel | |
| 8 | Pulse once to confirm success | "Expense saved", "Payment recorded" |
| 9 | Ease-in-out for moves between positions | `--ease-in-out` |
| 10 | Popovers start at scale 0.95, never 0 | sheets: translateY + 0.98 |
| 11 | Enter from the trigger (transform-origin at the button) | menus, sheets |
| 12 | Exit back to the trigger | |
| 13 | No bounce/overshoot on everyday UI | bounce only on the settled "完" moment, if ever |
| 14 | Don't tween while the user is dragging; track the pointer directly | |
| 15 | Exits ~20% shorter than entrances | `--dur-out: 160ms` |
| 16 | Overlap old and new states (no blank beat between) | view and tab changes |
| 17 | Use masks/clips for reveals and colour changes | segmented control fill, tab switch |
| 18 | Crossfade icons with scale + opacity + a little blur | check marks, copy → copied |
| 19 | Rubber-band past bounds (diminishing resistance) | sheet drag, overscroll |
| 20 | Reuse the same element when it expands (shared element / layoutId) | volume card → volume header |

Always: everything is instant under `prefers-reduced-motion: reduce`; no animation delays the first paint; no looping ambient motion.
