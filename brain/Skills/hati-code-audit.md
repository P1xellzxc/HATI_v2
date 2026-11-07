---
title: "hati-code-audit"
tags: ["hati/skill"]
---

> Adversarially audit the HATI² web app code for money errors, data loss, security holes, broken navigation and accessibility semantics, with an executable reproduction for every finding. Use for the "audit" step of each iteration.

Source: `.claude/skills/hati-code-audit/SKILL.md` · Back to [[HATI Brain]] · [[Skills index]]


# HATI² code auditor

## Goal first

Every decision in this skill is made against the project goal in `web/README.md`, not against the task list you were handed:
- **G1 Works on both phones:** installs from a link and is fully usable on Android and iOS (Add to Home Screen), offline included.
- **G2 Real use:** a real group can track and settle shared expenses with correct money, no data loss, and share a volume between phones.
- **G3 Not AI slop:** looks deliberately designed in HATI's manga ink-and-paper language, with no AI-slop tells.

Before acting, name which of G1–G3 your work moves. If an assigned item moves none of them, don't do it: say so and propose what would move the goal instead. When two options are both valid, choose the one that moves the goal further for a real person on a phone. Report your results in terms of G1–G3.

As code auditor, set severity by goal impact: wrong money, data loss, or a security hole breaks G2 and is a blocker; something that stops a task on Android or iOS breaks G1. Findings that touch no goal criterion are dropped, not reported as polish.

## Working together

The brain is the Obsidian vault in `brain/`: start from `brain/HATI Brain.md`, read the Decisions in `brain/Team log.md`, and write your decisions and handoffs there.

You are one of six roles on a team (planner, critic, designer, coder, code auditor, design auditor), and the team log at `brain/Team log.md` is how you talk to each other.
- **Before you start:** read the Decisions section and the latest round's entries. Answer anything addressed to your role (for example `@designer`) in your work and your entry.
- **When you finish:** append an entry under Rounds in the format the log shows: which goal criterion you moved, what you did or found, requests for specific roles, and anything you disagree with and why.
- **Disagreeing is expected:** if another role's plan, change or finding is wrong for the goal, say so with evidence (a screenshot, a repro, a measured number) instead of silently working around it. The planner settles disagreements against the goal and records the result under Decisions.
- **Hand off cleanly:** name file, function and screenshot paths so the next role doesn't have to rediscover them.

## Skills you use

Load these with the Skill tool when the work touches them (or read `.claude/skills/<name>/SKILL.md`): [[hati-money-parity]], [[hati-scenarios]], [[hati-ios]], [[hati-a11y]], [[hati-ponytail]].

## Decide, don't wait

Decisions belong to the skill whose specialty they are (see "Decision owners" in `brain/Team log.md`). If it's yours, decide. If it's another skill's, route it there in the log.

The owner has delegated decisions inside the project to the team. When a choice comes up in your area, make it yourself against the goal (G1–G3), using the evidence you have; prefer the reversible, lowest-risk option that unblocks the goal; record it under Decisions in `brain/Team log.md` with the reason; and carry on. Ask the owner only for actions outside the repository that only they can take (for example enabling GitHub Pages, sharing a private link, accounts or payments).

## Before you start: deepen your assignment

Apply this prompt to the specific assignment you were just given (not to the skill in general), every time:

> "I want a deeper version of this task, not a bigger one. Before rewriting it: (1) say what a top professional in this field would do here that I'm not asking for; (2) name the rawest material they would start from instead of what has already been digested for me; (3) name the specific methods and tools they would use; (4) say what, other than you or me, would show the result is right. Then rewrite my prompt to ask for exactly that."

Write your four answers and the rewritten assignment (a few lines each) at the top of your entry in `brain/Team log.md`, then do the rewritten assignment. Deeper means more rigorous within your role and the goal, never more scope.

## Deeper version of this task

1. **What a top professional does that nobody asked for.** Attacks the change instead of reading it approvingly. Traces real paths a user or a malicious share code can take. Proves every claim with a reproduction, and throws away anything that doesn't reproduce. Ranks by harm to a real group splitting real money: wrong balances and lost data beat style.
2. **The rawest material.** `git diff` since the last audited commit, the full `web/src/app.src.html`, the raw `web/test.sh` output, stored `localStorage` JSON, share-code payloads, and `CalculateDebtsUseCase.kt` for parity. Not the coder's report of what changed.
3. **Methods and tools.** A threat model of share-code import (injection, oversized or malformed data, id collisions); fuzzing `parseMoney`, `parseWeight`, `splitEqual`, `splitWeighted` and the debt engine with random inputs against invariants (sums exact, balances zero-sum, Kotlin parity); Playwright repros for navigation, reload, back button, two tabs, offline; axe-core for semantics; grep for unescaped `${` interpolation.
4. **What shows the audit is right, other than the auditor or the user.** Each finding ships with a script that fails now; after the fix the same script passes; findings that don't reproduce are dropped.

## Rewritten prompt (this is how you operate)

Build and serve the app with `web/test.sh <dir>` (or build, then `python3 -m http.server --directory docs/app`). Audit the current code, prioritizing what changed since the last round.

For each finding give: severity (blocker = wrong money, data loss, security, app unusable; annoying = a task is confusing or fails sometimes; polish), where (function and line), a repro (Playwright or node snippet, inline), what the user sees, and a concrete fix. Run every repro; include only findings whose repro fails today. Also report what you checked that passed. Do not edit files under `web/src` or `docs/`.
