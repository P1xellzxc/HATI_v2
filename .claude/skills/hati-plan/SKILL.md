---
name: hati-plan
description: Plan the next HATI² iteration toward the goal (installable on Android and iOS, usable for real group expenses, not AI-slop-looking). Use at the start of each plan→do→audit→revise round, or when audit findings need to be turned into a ranked, testable work list.
---

# HATI² planner

## Deeper version of this task

1. **What a top professional does that nobody asked for.** A senior product engineer plans from evidence of failure, not from a feature wish list. They define when the whole effort is *done* so the loop can stop, cap each iteration so it can actually finish, write acceptance criteria that a script can check, and keep a decision log so later rounds don't re-argue settled questions. They also cut: an item that doesn't move a goal criterion doesn't go in.
2. **The rawest material.** Not summaries of the app. The running app itself (fresh screenshots of every view at 360 and 390 px, light and dark), `web/src/app.src.html`, the raw output of `web/test.sh`, the auditors' raw findings from the last round, `git log -p` since the last round, and the Android domain code the web app must match (`domain/usecase/CalculateDebtsUseCase.kt`).
3. **Methods and tools.** Map the core job end to end (create a volume → add people → add expenses → see who owes whom → settle → share with someone on another phone) and find where it breaks or confuses. Rank items by user harm × reach ÷ effort. Write each acceptance criterion as Given/When/Then so it becomes an e2e check. At most 5 items per iteration.
4. **What shows the plan is right, other than the planner or the user.** Each item's acceptance check fails before the change and passes after, in `web/test.sh`. Audit severity counts fall from one round to the next. A cold reader can tell from the first screen what the app does.

## Rewritten prompt (this is how you operate)

Read the goal and invariants in `web/README.md`. Gather the raw material above yourself; take screenshots with Playwright (`require('playwright')`, `devices['iPhone 13']`) against a server from `web/test.sh` or `python3 -m http.server --directory docs/app`. Read last round's audit findings verbatim.

Then produce a plan for ONE iteration:
- **Goal status:** for each of the three done criteria in `web/README.md`, say met / not met, with the evidence.
- **Items (max 5)**, ranked. For each: the problem as the user experiences it (cite the screenshot or finding), the change, which file and function, the owner persona (`hati-code` or `hati-design`), and an acceptance check written so it can be added to `web/tests/e2e.js` or a design-audit rubric line.
- **Not doing this round:** what you cut and why.
- **Decisions:** anything settled that later rounds should not reopen.

Do not edit code. Do not add features that no goal criterion needs. If every done criterion is met, say so plainly and return zero items.
