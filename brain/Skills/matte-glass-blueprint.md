---
title: "matte-glass-blueprint"
tags: ["hati/skill"]
---

> Design blueprint for "matte glass on calm paper" interfaces — tokens, component recipes, states, charts, accessibility checks and anti-patterns — reusable in any web or hybrid app. Use when starting a new app's visual design, restyling an existing one to matte glass, adding a component (card, button, chip grid, field, sheet, chart) to a matte glass app, or auditing one for separation, contrast and slop.

Source: `.claude/skills/matte-glass-blueprint/SKILL.md` · Back to [[HATI Brain]] · [[Skills index]]


# Matte glass blueprint

A portable design system distilled from HATI² (2026-11). It is **project-agnostic**: every colour comes from five brand inputs, every component from one slab recipe. Copy this folder into another project's `.claude/skills/`, then follow the procedure.

## The idea in one paragraph

A **calm two-tone paper** page (nothing busy behind the glass) holds **matte glass panes**: near-opaque bodies with a fine grain, a lit 1px top edge, a 1px outer edge line, round corners and a **soft shadow falling down and to the right** (light from the top left). One accent glass marks the one or two things that matter per screen (the main number, the main action). Chosen options **sink and invert**. Real blur is spent only on things that float (top bar, sheets). Separation between page and cards is guaranteed by three independent cues: lightness step, edge line, shadow.

## Inputs (the only per-project decisions)

| Token | Meaning | Rule |
|---|---|---|
| `--paper` | lightest neutral | near white, slight hue bias allowed |
| `--ink` | text and lines | ≥ 7:1 on `--paper` |
| `--brand` | second brand colour | tints the page and glass (12–16%) |
| `--accent` | the one highlight | light enough for `--ink` text on it (≥ 4.5:1) |
| `--muted` | grey text | ≥ 4.5:1 on the page **after** tinting |

Multi-theme apps (palettes): define these five per theme; nothing else changes. Dark mode is **designed**, not inverted: see `references/tokens.css`.

## Procedure

1. **Tokens first.** Paste `references/tokens.css`, set the five inputs. Do not invent colours in components.
2. **Page.** `body` gets the two-tone paper (`--page-a` → `--page-b`, one soft diagonal). No art, blobs, speed lines or photos behind glass (they turn matte glass into noise).
3. **Components from the slab recipe.** Paste `references/components.css`. Cards: `--t: 8px`, radius 16px. Controls: `--t: 4px`, radius 12px. Accent only on `.is-accent`. See `references/patterns.md` for markup.
4. **States.** Pressed = shadow shrinks to contact (`--t: 0`), translate ≤ 2px. Chosen = inverted fill (`--fg` on `--bg`), inset shade, no hue-only cue. Disabled = 55% opacity, no shadow. Focus = 3px outline in `--fg`, offset 2px (never removed by a state rule).
5. **Layout rules.** One inner inset for all card text (16px). One rhythm: 16px between blocks (measured from the bottom of the shadow's slab), 8px from a label to its block, 12px between form fields. Text edges of every card align on two vertical lines. Equal-width choice grids (see *Balanced rows*).
6. **Charts** live on their own glass slab; key and "All the numbers" on a second slab; full data opens in a glass sheet. See `references/charts.md`.
7. **Check** (do not eyeball): run `scripts/check_contrast.js` against the running app in every theme; screenshot light + dark at 360/390px; walk the anti-pattern list.

## The slab recipe (the core)

```css
.slab {
  --t: 8px;                                   /* shadow depth: 8 cards, 4 controls, 0 pressed */
  border: 0; border-radius: 16px;
  background:
    linear-gradient(var(--g-lip), var(--g-lip)) 0 0 / 100% 1px no-repeat,   /* lit top edge */
    var(--grain),                                                           /* matte */
    linear-gradient(180deg, var(--g-top), var(--g-foot)) var(--g-foot);    /* body, deeper at the foot */
  box-shadow:
    inset 0 0 0 1px var(--g-rim),             /* light rim inside */
    0 0 0 1px var(--g-line),                  /* edge line: separation without shadow */
    var(--t) calc(1.4 * var(--t)) calc(2.6 * var(--t)) calc(-.5 * var(--t)) var(--g-shadow),
    1px 2px 3px var(--g-contact);             /* contact shadow */
}
```

Why each layer exists: the **lip** says "glass catches light", the **grain** says "matte, not shiny", the **gradient** gives volume, the **edge line** guarantees separation on any page tint, the **soft shadow** gives depth, the **contact** keeps it seated. Remove any one and the pane reads flat or floats.

## Separation guarantee (owner rule: "there must be separation")

A card must differ from the page by **at least two** of: lightness step (page carries 7% ink + 14% brand; card top is pure paper), 1px edge line (ink 14% light / chalk 16% dark), shadow (ink 38% light / black 62% dark). Inside a sheet, the sheet takes the page tint so cards inside it still separate.

## Blur budget

`backdrop-filter` only on floating layers (top bar, sheets, tooltips). Never on list rows or cards that scroll: it costs ~1 s of compositor time per screen of cards on mid phones. Low-end devices (`hardwareConcurrency ≤ 4` or `deviceMemory ≤ 2` → `:root.lowfx`) and `prefers-reduced-transparency` get solid fills.

## Balanced rows (choice grids, member lists)

Equal buttons that always fill the row, for any count: at most N per row (4 for short labels, 2 for two-line chips), rows split as evenly as possible (5 → 3+2, 7 → 4+3), each row's items share its full width, long labels end in "…" or wrap inside. Code in `references/patterns.md`.

## Accessibility (non-negotiable)

- Text ≥ 4.5:1 on its real background in every theme; field and control edges ≥ 3:1 (WCAG 1.4.11). Give every glass body an **opaque base colour** (the last layer of `background`) so checkers and screen readers' contrast tools can measure it.
- Chosen state never relies on hue alone (inverted fill ≥ 3:1 vs unchosen).
- Tap targets ≥ 44px; focus visible; radiogroups with roving tabindex for choice grids; sheets are `role=dialog` with focus moved in and restored on close; Esc closes.
- Motion: presses 90 ms, entrances 200 ms, exits 160 ms, transform/opacity only; `prefers-reduced-motion` removes movement.

## Anti-patterns (if you see one, it is wrong)

- Busy art, gradient blobs or photos behind glass.
- Cards the same colour as the page (no separation).
- Hard 0-blur offset slabs mixed with soft glass shadows (two lighting stories).
- Stacked "decorative" sheets peeking above a card (reads as tabs).
- Shadows falling in different directions on one screen.
- Blur on scrolling lists.
- Accent on everything (accent is for one or two elements per screen).
- `:is()` / `:not()` specificity surprises: a generic control rule silently out-ranking `.primary` (keep variant selectors at least as specific as the base rule).
- A `background` shorthand written after `background-color` (it resets the base colour to transparent).
- Chart lines thicker than 1.5px, or more than 4 series + an average.

## Files

- `references/tokens.css` — light + dark tokens from the five inputs.
- `references/components.css` — page, slab, controls, chosen, fields, top bar, sheet, chip grid, chart slab.
- `references/patterns.md` — markup and JS for balanced rows, sheets, chosen states, alignment rules.
- `references/charts.md` — line chart per category + moving average, colour validation, key slab, numbers sheet, pinned first column.
- `scripts/check_contrast.js` — Playwright script: every visible text vs its effective background, per theme.
- `examples/demo.html` — one page using everything; open it to see the system before adopting it.

## Decide, don't drift

When a new component is needed: build it from the slab recipe with `--t` 8 (card) or 4 (control); give it an opaque base colour; check it against the anti-pattern list; measure contrast. Record the decision where the project keeps its decisions.
