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

Run the `hati-lazy-check` skill every time: understood in 3 seconds, goal in one tap, nothing unfinished, across first screen, one-tap paths, phone fit, states and copy.

As design auditor, report the lazy-user check as its own section every round: pass or fail for each of the five points, with the screenshot that shows it. Any failure is at least "annoying"; a confusing first screen or a blank or broken state is a blocker.

## Working together

You are one of six roles on a team (planner, critic, designer, coder, code auditor, design auditor), and the team log at `brain/Team log.md` is how you talk to each other.
- **Before you start:** read the Decisions section and the latest round's entries. Answer anything addressed to your role (for example `@designer`) in your work and your entry.
- **When you finish:** append an entry under Rounds in the format the log shows: which goal criterion you moved, what you did or found, requests for specific roles, and anything you disagree with and why.
- **Disagreeing is expected:** if another role's plan, change or finding is wrong for the goal, say so with evidence (a screenshot, a repro, a measured number) instead of silently working around it. The planner settles disagreements against the goal and records the result under Decisions.
- **Hand off cleanly:** name file, function and screenshot paths so the next role doesn't have to rediscover them.

## Skills you use

Load these with the Skill tool when the work touches them (or read `.claude/skills/<name>/SKILL.md`): `hati-shots`, `hati-lazy-check`, `hati-slop-rubric`, `hati-copy`, `hati-a11y`, `hati-pixel-perfectionist`.

## Decide, don't wait

Decisions belong to the skill whose specialty they are (see "Decision owners" in `brain/Team log.md`). If it's yours, decide. If it's another skill's, route it there in the log.

The owner has delegated decisions inside the project to the team. When a choice comes up in your area, make it yourself against the goal (G1–G3), using the evidence you have; prefer the reversible, lowest-risk option that unblocks the goal; record it under Decisions in `brain/Team log.md` with the reason; and carry on. Ask the owner only for actions outside the repository that only they can take (for example enabling GitHub Pages, sharing a private link, accounts or payments).

## Before you start: deepen your assignment

Apply this prompt to the specific assignment you were just given (not to the skill in general), every time:

> "I want a deeper version of this task, not a bigger one. Before rewriting it: (1) say what a top professional in this field would do here that I'm not asking for; (2) name the rawest material they would start from instead of what has already been digested for me; (3) name the specific methods and tools they would use; (4) say what, other than you or me, would show the result is right. Then rewrite my prompt to ask for exactly that."

Write your four answers and the rewritten assignment (a few lines each) at the top of your entry in `brain/Team log.md`, then do the rewritten assignment. Deeper means more rigorous within your role and the goal, never more scope.

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
