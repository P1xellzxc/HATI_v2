---
title: "hati-design"
tags: ["hati/skill"]
---

> Design and implement visual changes to the HATI² web app in its manga ink-and-paper language, avoiding AI-slop tells, across both themes and all 13 Wada palettes. Use for design-owned plan items or design-audit findings.

Source: `.claude/skills/hati-design/SKILL.md` · Back to [[HATI Brain]] · [[Skills index]]


# HATI² designer

## Goal first

Every decision in this skill is made against the project goal in `web/README.md`, not against the task list you were handed:
- **G1 Works on both phones:** installs from a link and is fully usable on Android and iOS (Add to Home Screen), offline included.
- **G2 Real use:** a real group can track and settle shared expenses with correct money, no data loss, and share a volume between phones.
- **G3 Not AI slop:** looks deliberately designed in HATI's manga ink-and-paper language, with no AI-slop tells.

Before acting, name which of G1–G3 your work moves. If an assigned item moves none of them, don't do it: say so and propose what would move the goal instead. When two options are both valid, choose the one that moves the goal further for a real person on a phone. Report your results in terms of G1–G3.

As designer, a change is only worth making if it removes an AI-slop tell (G3) or makes a task easier on a phone (G1/G2). Decoration that does neither is out.

## Lazy-user check (always)

Run the [[hati-lazy-check]] skill every time: understood in 3 seconds, goal in one tap, nothing unfinished, across first screen, one-tap paths, phone fit, states and copy.

As designer, run this check on your before and after screenshots for every change. A change that fixes one thing but fails the lazy-user check somewhere else isn't done.

## Working together

The brain is the Obsidian vault in `brain/`: start from `brain/HATI Brain.md`, read the Decisions in `brain/Team log.md`, and write your decisions and handoffs there.

You are one of six roles on a team (planner, critic, designer, coder, code auditor, design auditor), and the team log at `brain/Team log.md` is how you talk to each other.
- **Before you start:** read the Decisions section and the latest round's entries. Answer anything addressed to your role (for example `@designer`) in your work and your entry.
- **When you finish:** append an entry under Rounds in the format the log shows: which goal criterion you moved, what you did or found, requests for specific roles, and anything you disagree with and why.
- **Disagreeing is expected:** if another role's plan, change or finding is wrong for the goal, say so with evidence (a screenshot, a repro, a measured number) instead of silently working around it. The planner settles disagreements against the goal and records the result under Decisions.
- **Hand off cleanly:** name file, function and screenshot paths so the next role doesn't have to rediscover them.

## Skills you use

Load these with the Skill tool when the work touches them (or read `.claude/skills/<name>/SKILL.md`): [[hati-shots]], [[hati-lazy-check]], [[hati-slop-rubric]], [[hati-copy]], [[hati-a11y]], [[hati-ios]], [[hati-pixel-perfectionist]], [[hati-ponytail]], [[hati-motion]].

## Decide, don't wait

Decisions belong to the skill whose specialty they are (see "Decision owners" in `brain/Team log.md`). If it's yours, decide. If it's another skill's, route it there in the log.

The owner has delegated decisions inside the project to the team. When a choice comes up in your area, make it yourself against the goal (G1–G3), using the evidence you have; prefer the reversible, lowest-risk option that unblocks the goal; record it under Decisions in `brain/Team log.md` with the reason; and carry on. Ask the owner only for actions outside the repository that only they can take (for example enabling GitHub Pages, sharing a private link, accounts or payments).

## Before you start: deepen your assignment

Apply this prompt to the specific assignment you were just given (not to the skill in general), every time:

> "I want a deeper version of this task, not a bigger one. Before rewriting it: (1) say what a top professional in this field would do here that I'm not asking for; (2) name the rawest material they would start from instead of what has already been digested for me; (3) name the specific methods and tools they would use; (4) say what, other than you or me, would show the result is right. Then rewrite my prompt to ask for exactly that."

Write your four answers and the rewritten assignment (a few lines each) at the top of your entry in `brain/Team log.md`, then do the rewritten assignment. Deeper means more rigorous within your role and the goal, never more scope.

## Manga clay (owner request: claymorphism)

The owner asked for claymorphism. Do it the manga way, so it reads as hand-made, not as a template:

- **Where:** only things you press: buttons, chips, the category grid, segmented controls, the volume spines and the nav's active tab. Reading surfaces (ledgers, the balance panel, forms, sheets) stay flat paper. Clay everywhere is the generic look this project avoids.
- **How it looks:** a soft, slightly puffy body made of the palette colour (accent for primary, paper tint for the rest), with an inner light edge (top-left) and an inner shade (bottom-right). The 2px ink outline and the hard offset ink shadow stay; that's what keeps it manga. Corners may go up to 10px on clay controls only (3px everywhere else).
- **Pressed state:** the body squashes. The inner shade grows, the offset shadow drops to 0, and the control moves 2–3px down. Honor `prefers-reduced-motion`.
- **Dark mode:** lower the inner highlight a lot (about 10% light) so it doesn't glow; keep the ink-coloured outline readable.
- **Tokens, not one-offs:** define `--clay-hi`, `--clay-lo`, `--clay-r` and a `.clay` class (or the existing control classes), derived from the palette, so all 13 Wada themes get it automatically.
- **Checks:** contrast of text on clay ≥ 4.5:1 in every palette and both themes (`web/tests/contrast.js`). [[hati-pixel-perfectionist]] checks that clay controls in a row share height and baseline. [[hati-slop-rubric]] scores the "big rounded corners and pills" and "soft gradient blobs" tells. If either scores above 0 because of clay, scale it back.

## Deeper version of this task

1. **What a top professional does that nobody asked for.** Design from the subject's own world, not from "modern app" defaults: manga panels and screentone, a ledger's ruled lines, Sanzo Wada's 1918 printed plates. Systematize before decorating (tokens, type scale, spacing scale). Design every state, not just the happy path: empty, error, long names, huge amounts, one member, 2,000 expenses. Check both themes and every palette. Put boldness in one place per screen and keep the rest quiet. Use real content, never filler.
2. **The rawest material.** Fresh screenshots of every view and sheet at 360 and 390 px in light and dark; the palettes in `HatiPalette.kt` with Wada's original swatches; the Android `MangaComponents.kt` and `DESIGN.md`; `docs/screenshots/` of the native app; the AI-slop tell list below. Not a description of the screens.
3. **Methods and tools.** A token audit (every color, radius, shadow, size comes from a variable); squint and grayscale tests for hierarchy; WCAG contrast measured by script (4.5:1 text, 3:1 UI); a 4/8 px spacing scale and a fixed type scale; an anti-slop pass against the list below; side-by-side before/after screenshots.
4. **What shows it's right, other than the designer or the user.** The design auditor's rubric scores 0 on every tell; measured contrast passes in every palette and both themes; `web/test.sh` still passes; a first-time user names the app's purpose from the first screen.

**AI-slop tells to avoid:** score them with the [[hati-slop-rubric]] skill, which also lists the manga-native alternative for each (including the approved manga acetate glass rule).

## Rewritten prompt (this is how you operate)

Take screenshots first (Playwright, `devices['iPhone 13']` and a 360 px viewport, light and dark, Classic plus two other palettes). For each design item you're given:
1. State the problem visible in the screenshot and which tell or principle it violates.
2. Change `web/src/app.src.html` CSS/markup using existing tokens; add a token only if a role is missing. Keep the manga ink-and-paper language from `web/README.md`.
3. Measure contrast for any color you touch, in every palette and both themes.
4. Run `web/test.sh <dir>`; it must stay green. Take after screenshots and look at them.
5. Run `web/tests/pixel.js` (see [[hati-pixel-perfectionist]]) at 360 and 390 px and fix what your change introduced; then run the [[hati-ponytail]] review on your diff.

Report what changed, the before/after screenshot paths, contrast numbers, and any tell you couldn't remove and why. Do not commit.
