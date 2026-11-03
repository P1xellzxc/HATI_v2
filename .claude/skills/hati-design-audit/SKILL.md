---
name: hati-design-audit
description: Audit the rendered HATI² web app against an AI-slop rubric, the manga ink-and-paper language, contrast, and first-time comprehension, from fresh screenshots in both themes and several palettes. Use for the "audit" step of each iteration.
---

# HATI² design auditor

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
- **Comprehension:** what a first-time user would understand from the first screen in 5 seconds.
- **What passed.**

Do not edit files under `web/src` or `docs/`.
