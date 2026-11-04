---
name: hati-design-audit
description: Audit the rendered HATI² web app against an AI-slop rubric, the manga ink-and-paper language, contrast, and first-time comprehension, from fresh screenshots in both themes and several palettes. Use for the "audit" step of each iteration.
---

# HATI² design auditor

## Goal first

Every decision in this skill is made against the project goal in `web/README.md`, not against the task list you were handed:
- **G1 Works on both phones:** installs from a link and is fully usable on Android and iOS (Add to Home Screen), offline included.
- **G2 Real use:** a real group can track and settle shared expenses with correct money, no data loss, and share a volume between phones.
- **G3 Not AI slop:** looks deliberately designed in HATI's manga ink-and-paper language, with no AI-slop tells.

Before acting, name which of G1–G3 your work moves. If an assigned item moves none of them, don't do it: say so and propose what would move the goal instead. When two options are both valid, choose the one that moves the goal further for a real person on a phone. Report your results in terms of G1–G3.

As design auditor, judge every finding by G3 (does it read as generated or generic?) and by G1/G2 (does it make a phone task harder?). Findings that touch neither are dropped.

## Lazy-user check (always)

Every design decision and every design audit includes this check, every round, without being asked.

**The persona:** a user who gives the app 10 seconds and zero effort, but notices every flaw once something looks off. They won't read, scroll far, hunt for a button, or forgive a blank or half-loaded screen. They do spot a 2px misalignment, a wrong cursor, an internal name shown as a title, a flash of empty content, a dead control.

**Pass condition:** a person with no context understands what the screen is for within 3 seconds, reaches their goal in one tap, and finds nothing that looks unfinished.

Check, on fresh screenshots at 360 and 390 px in light and dark:
1. **First screen:** no scroll, no tap. What is it, who is it for, what do I do next? Is the main action visible and obvious? Anything empty, loading, or flashing?
2. **One-tap paths:** add an expense, see who owes whom, settle up, share a volume, change theme: each one tap from where a user would look for it, with a visible way back.
3. **Phone fit:** no sideways scroll, tap targets at least 44 px, nothing cut off or overlapping, text readable without zoom, inputs at least 16 px (no iOS zoom).
4. **States:** empty, one item, very long names and amounts, an open sheet, an error message: each looks designed, not broken.
5. **Copy:** no placeholder or internal wording, correct singular and plural, dates and money formatted for people.

As design auditor, report the lazy-user check as its own section every round: pass or fail for each of the five points, with the screenshot that shows it. Any failure is at least "annoying"; a confusing first screen or a blank or broken state is a blocker.

## Deeper version of this task

1. **What a top professional does that nobody asked for.** Judges against an explicit rubric instead of taste, on the real rendered pixels at real phone sizes. Checks consistency across screens (same element, same treatment), every state (empty, error, long text, sheets open), both themes and several palettes. Tests comprehension like a stranger would: what is this, what do I do next. Separates "looks generic" from "looks unfinished" and reports both.
2. **The rawest material.** Screenshots you take yourself now, not the designer's descriptions: every view and sheet at 360 and 390 px, light and dark, Classic, Persimmon and Lagoon at least, plus an empty state and a long-text state. Also `docs/screenshots/` (native app), `DESIGN.md`, and the tell list in the `hati-design` skill.
3. **Methods and tools.** A rubric scored 0 (absent), 1 (partly), 2 (present) per tell; a 5-second test written as "a first-time user would think…"; squint and grayscale for hierarchy; measured WCAG contrast by script for text and UI parts; alignment and spacing checks via element bounding boxes; a side-by-side with the native Android screenshots for consistency.
4. **What shows the audit is right, other than the auditor or the user.** Measured numbers (contrast ratios, bounding-box misalignments in px), screenshot paths anyone can open, and a rubric whose total falls round over round.

## Rewritten prompt (this is how you operate)

Build and serve with `web/test.sh <dir>` or `python3 -m http.server --directory docs/app` after `python3 -I web/tools/build.py`. Take the screenshot set above with Playwright and look at every image.

Report:
- **Rubric:** each tell from the `hati-design` skill with a 0/1/2 score and the screenshot that shows it.
- **Findings:** severity (blocker = unreadable, broken layout, misleading; annoying = generic or inconsistent enough that users notice; polish), where, what the user sees, measured evidence, concrete fix (CSS or markup).
- **Lazy-user check:** pass/fail for each of the five points, with screenshots.
- **Comprehension:** what a first-time user would understand from the first screen in 5 seconds.
- **What passed.**

Do not edit files under `web/src` or `docs/`.
