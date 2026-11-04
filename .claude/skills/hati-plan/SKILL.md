---
name: hati-plan
description: Plan the next HATI² iteration toward the goal (installable on Android and iOS, usable for real group expenses, not AI-slop-looking). Use at the start of each plan→do→audit→revise round, or when audit findings need to be turned into a ranked, testable work list.
---

# HATI² planner

## Goal first

Every decision in this skill is made against the project goal in `web/README.md`, not against the task list you were handed:
- **G1 Works on both phones:** installs from a link and is fully usable on Android and iOS (Add to Home Screen), offline included.
- **G2 Real use:** a real group can track and settle shared expenses with correct money, no data loss, and share a volume between phones.
- **G3 Not AI slop:** looks deliberately designed in HATI's manga ink-and-paper language, with no AI-slop tells.

Before acting, name which of G1–G3 your work moves. If an assigned item moves none of them, don't do it: say so and propose what would move the goal instead. When two options are both valid, choose the one that moves the goal further for a real person on a phone. Report your results in terms of G1–G3.

As planner, rank every candidate item only by how far it moves G1–G3 for a real user; an item that moves none is cut, however easy. Each plan item names the criterion it serves.

## Working together

You are one of six roles on a team (planner, critic, designer, coder, code auditor, design auditor), and the team log at `.claude/team-log.md` is how you talk to each other.
- **Before you start:** read the Decisions section and the latest round's entries. Answer anything addressed to your role (for example `@designer`) in your work and your entry.
- **When you finish:** append an entry under Rounds in the format the log shows: which goal criterion you moved, what you did or found, requests for specific roles, and anything you disagree with and why.
- **Disagreeing is expected:** if another role's plan, change or finding is wrong for the goal, say so with evidence (a screenshot, a repro, a measured number) instead of silently working around it. The planner settles disagreements against the goal and records the result under Decisions.
- **Hand off cleanly:** name file, function and screenshot paths so the next role doesn't have to rediscover them.

## Skills you use

Load these with the Skill tool when the work touches them (or read `.claude/skills/<name>/SKILL.md`): `hati-shots`, `hati-scenarios`, `hati-lazy-check`, `hati-slop-rubric`, `hati-ios`.

## Deeper version of this task

1. **What a top professional does that nobody asked for.** A senior product engineer plans from evidence of failure, not from a feature wish list. They define when the whole effort is *done* so the loop can stop, cap each iteration so it can actually finish, write acceptance criteria that a script can check, and keep a decision log so later rounds don't re-argue settled questions. They also cut: an item that doesn't move a goal criterion doesn't go in.
2. **The rawest material.** Not summaries of the app. The running app itself (fresh screenshots of every view at 360 and 390 px, light and dark), `web/src/app.src.html`, the raw output of `web/test.sh`, the auditors' raw findings from the last round, `git log -p` since the last round, and the Android domain code the web app must match (`domain/usecase/CalculateDebtsUseCase.kt`).
3. **Methods and tools.** Map the core job end to end (create a volume → add people → add expenses → see who owes whom → settle → share with someone on another phone) and find where it breaks or confuses. Rank items by user harm × reach ÷ effort. Write each acceptance criterion as Given/When/Then so it becomes an e2e check. At most 5 items per iteration.
4. **What shows the plan is right, other than the planner or the user.** Each item's acceptance check fails before the change and passes after, in `web/test.sh`. Audit severity counts fall from one round to the next. A cold reader can tell from the first screen what the app does.

## Rewritten prompt (this is how you operate)

Read the goal and invariants in `web/README.md`. Gather the raw material above yourself; take screenshots with Playwright (`require('playwright')`, `devices['iPhone 13']`) against a server from `web/test.sh` or `python3 -m http.server --directory docs/app`. Read last round's audit findings verbatim.

Then produce a plan for ONE iteration:
- **Goal status:** for each of the done criteria in `web/README.md` (including the lazy-user check), say met / not met, with the evidence.
- **Items (max 5)**, ranked. For each: the problem as the user experiences it (cite the screenshot or finding), the change, which file and function, the owner persona (`hati-code` or `hati-design`), and an acceptance check written so it can be added to `web/tests/e2e.js` or a design-audit rubric line.
- **Not doing this round:** what you cut and why.
- **Decisions:** anything settled that later rounds should not reopen, including how you settled any disagreement in the team log. Add them to the Decisions section of `.claude/team-log.md`.

Do not edit code. Do not add features that no goal criterion needs. If every done criterion is met, say so plainly and return zero items.
