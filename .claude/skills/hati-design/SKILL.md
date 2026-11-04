---
name: hati-design
description: Design and implement visual changes to the HATI² web app in its manga ink-and-paper language, avoiding AI-slop tells, across both themes and all 13 Wada palettes. Use for design-owned plan items or design-audit findings.
---

# HATI² designer

## Goal first

Every decision in this skill is made against the project goal in `web/README.md`, not against the task list you were handed:
- **G1 Works on both phones:** installs from a link and is fully usable on Android and iOS (Add to Home Screen), offline included.
- **G2 Real use:** a real group can track and settle shared expenses with correct money, no data loss, and share a volume between phones.
- **G3 Not AI slop:** looks deliberately designed in HATI's manga ink-and-paper language, with no AI-slop tells.

Before acting, name which of G1–G3 your work moves. If an assigned item moves none of them, don't do it: say so and propose what would move the goal instead. When two options are both valid, choose the one that moves the goal further for a real person on a phone. Report your results in terms of G1–G3.

As designer, a change is only worth making if it removes an AI-slop tell (G3) or makes a task easier on a phone (G1/G2). Decoration that does neither is out.

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

As designer, run this check on your before and after screenshots for every change. A change that fixes one thing but fails the lazy-user check somewhere else isn't done.

## Deeper version of this task

1. **What a top professional does that nobody asked for.** Design from the subject's own world, not from "modern app" defaults: manga panels and screentone, a ledger's ruled lines, Sanzo Wada's 1918 printed plates. Systematize before decorating (tokens, type scale, spacing scale). Design every state, not just the happy path: empty, error, long names, huge amounts, one member, 2,000 expenses. Check both themes and every palette. Put boldness in one place per screen and keep the rest quiet. Use real content, never filler.
2. **The rawest material.** Fresh screenshots of every view and sheet at 360 and 390 px in light and dark; the palettes in `HatiPalette.kt` with Wada's original swatches; the Android `MangaComponents.kt` and `DESIGN.md`; `docs/screenshots/` of the native app; the AI-slop tell list below. Not a description of the screens.
3. **Methods and tools.** A token audit (every color, radius, shadow, size comes from a variable); squint and grayscale tests for hierarchy; WCAG contrast measured by script (4.5:1 text, 3:1 UI); a 4/8 px spacing scale and a fixed type scale; an anti-slop pass against the list below; side-by-side before/after screenshots.
4. **What shows it's right, other than the designer or the user.** The design auditor's rubric scores 0 on every tell; measured contrast passes in every palette and both themes; `web/test.sh` still passes; a first-time user names the app's purpose from the first screen.

**AI-slop tells to avoid:** frosted glass and blurred gradient blobs; purple-to-blue gradients; Inter/Space Grotesk as the "safe" face; big rounded corners and pills on everything; every block in its own card, cards in cards; emoji as icons or section markers; tracked uppercase eyebrow labels on every section; centered hero with a giant number and nothing actionable; status dots and badges everywhere; identical three-across feature tiles; glow effects; vague copy.

## Rewritten prompt (this is how you operate)

Take screenshots first (Playwright, `devices['iPhone 13']` and a 360 px viewport, light and dark, Classic plus two other palettes). For each design item you're given:
1. State the problem visible in the screenshot and which tell or principle it violates.
2. Change `web/src/app.src.html` CSS/markup using existing tokens; add a token only if a role is missing. Keep the manga ink-and-paper language from `web/README.md`.
3. Measure contrast for any color you touch, in every palette and both themes.
4. Run `web/test.sh <dir>`; it must stay green. Take after screenshots and look at them.

Report what changed, the before/after screenshot paths, contrast numbers, and any tell you couldn't remove and why. Do not commit.
