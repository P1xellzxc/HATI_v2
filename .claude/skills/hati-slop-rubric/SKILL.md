---
name: hati-slop-rubric
description: Score the rendered HATI² web app against the AI-slop rubric (0 absent, 1 partly, 2 present per tell) with screenshot evidence, and know the manga-native alternatives. Use in every design step and design audit.
---

# HATI² AI-slop rubric

## Goal first

Use this skill only for work that moves the project goal in `web/README.md`: **G1** works on Android and iOS from a link (offline included), **G2** real group use with correct money and no data loss, **G3** not AI slop. Say which criterion your use of this skill serves. Done criterion 3 in `web/README.md`; this is G3.

## Decide, don't wait

Decisions belong to the skill whose specialty they are (see "Decision owners" in `brain/Team log.md`). If it's yours, decide. If it's another skill's, route it there in the log.

The owner has delegated decisions inside the project to the team. When a choice comes up in your area, make it yourself against the goal (G1–G3), using the evidence you have; prefer the reversible, lowest-risk option that unblocks the goal; record it under Decisions in `brain/Team log.md` with the reason; and carry on. Ask the owner only for actions outside the repository that only they can take (for example enabling GitHub Pages, sharing a private link, accounts or payments).

## Before you start: deepen your assignment

Apply this prompt to the specific assignment you were just given (not to the skill in general), every time:

> "I want a deeper version of this task, not a bigger one. Before rewriting it: (1) say what a top professional in this field would do here that I'm not asking for; (2) name the rawest material they would start from instead of what has already been digested for me; (3) name the specific methods and tools they would use; (4) say what, other than you or me, would show the result is right. Then rewrite my prompt to ask for exactly that."

Write your four answers and the rewritten assignment (a few lines each) at the top of your entry in `brain/Team log.md`, then do the rewritten assignment. Deeper means more rigorous within your role and the goal, never more scope.

## Deeper version of this task
1. **Top professional:** names the specific tell and where it appears instead of saying "looks generic", and replaces it with something from the subject's own world.
2. **Rawest material:** screenshots from `hati-shots`; the source CSS; the native app's look in `docs/screenshots/` and `ui/components/MangaComponents.kt`.
3. **Methods/tools:** score each tell 0/1/2 with a screenshot path; squint and grayscale tests for hierarchy; compare against the manga alternatives below.
4. **What shows it's right:** every tell scores 0 in both themes at 360 and 390 px, with evidence anyone can open.


## Rewritten prompt (this is how you operate)

For your assignment, score every tell in the table below 0, 1 or 2 from fresh screenshots in both themes at 360 and 390 px, citing the screenshot for each score. For every tell above 0, name the manga-native replacement and the exact CSS or markup change.

## Tells → manga-native alternative
| Tell | Use instead |
|---|---|
| Frosted glass over blurred gradient blobs | Flat paper; if translucency is wanted, "acetate": translucent floating layers only (top bar, nav, sheets) with a screentone dot texture and a 2px ink edge, no blur clouds, no glow |
| Purple-to-blue gradients, glow | Wada palette accent used sparingly; screentone for texture |
| Inter / Space Grotesk as the safe face | Black Han Sans display + DM Sans body (current) |
| Big rounded corners and pills everywhere | 3px corners; pills only where shape carries meaning |
| Every block a card, cards in cards | Ruled ledgers; one panel per group |
| Emoji as icons or section markers | Drawn line icons; kaomoji only in empty states (manga vernacular) |
| Tracked uppercase eyebrows on every section | Sentence-case headings in the display face |
| Centered hero number with no action | Left-aligned balance that says who to pay |
| Status dots/badges everywhere | Color only for money direction (owed/owe) |
| Identical three-across tiles | Layout that follows the data |
| Vague marketing copy | `hati-copy` voice |

## Decision on glass
The owner approved exploring **manga acetate glass** within the rule above: translucent floating layers with screentone and ink edges only, never on scrolling content, never over gradient blobs. If the rubric or the lazy-user check flags it, revert it.

## Decision on clay
The owner asked for claymorphism. It is allowed as **manga clay** (see `hati-design`): a clay body with inner light and shade, on pressable controls only, keeping the 2px ink outline and hard offset shadow, corners up to 10px on those controls only. Clay on reading surfaces, clay without the ink outline, pastel blobs, or rounded pills everywhere still score as tells; flag and scale back.
