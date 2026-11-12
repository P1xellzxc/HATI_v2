# Patterns

## Screen anatomy

```
[ top bar: frosted, sticky ]
label ─────────────────────
[ accent card: the main number ]          ← one per screen
[ primary button ] [ button ] [ button ]  ← accent on one only
label ─────────────────────
[ card: ledger rows ]
label ─────────────────────
[ chart slab ]
[ key slab: legend chips · "All the numbers ›" ]
```

Rhythm: 16px between blocks, 8px label → block, 12px between form fields. Every card's text starts 16px in, so all text on the screen shares one left edge and one right edge.

## Balanced rows (choice grids, people, tags)

```js
// At most `max` per row, rows as even as possible: 5 → 3+2, 6 → 3+3, 7 → 4+3, 8 → 4+4
function balancedRows(n, max = 4) {
  const rows = Math.max(1, Math.ceil(n / max)), base = Math.floor(n / rows), extra = n % rows;
  return Array.from({ length: rows }, (_, i) => base + (i < extra ? 1 : 0));
}
function rowsHtml(items, max = 4) {           // items: HTML strings
  let k = 0;
  return `<div class="rows">${balancedRows(items.length, max)
    .map(c => `<div class="row" style="--cols:${c}">${items.slice(k, k += c).join('')}</div>`).join('')}</div>`;
}
```

- Short labels (payers, methods): `max = 4`. Two-line chips (avatar + name + amount): `max = 2`.
- A choice grid is a `role="radiogroup"`; items are `role="radio"` with `aria-checked`; one tab stop on the chosen item; arrow keys move across rows and wrap.
- A long name must not widen the page: `.rows` uses `minmax(0, 1fr)`; bare text inside an item wraps (`overflow-wrap: anywhere`) or ellipsizes.

## Chosen, pressed, disabled

| State | Look | Why |
|---|---|---|
| Rest | slab, `--t` 4 (controls) / 8 (cards) | depth by shadow |
| Pressed | `--t: 0`, translate 1px | lands on its contact shadow |
| Chosen | inverted fill + inset shade | not hue alone; ≥ 3:1 vs unchosen |
| Disabled | opacity .55, no shadow | flat = not pressable |
| Focus | 3px `--fg` outline, offset 2px | survives every state |

## Sheet (details, forms, "All the numbers")

```html
<div class="scrim">
  <section class="sheet" role="dialog" aria-modal="true" aria-labelledby="sh-t">
    <header style="display:flex;justify-content:space-between;align-items:center">
      <h2 id="sh-t">Spent over time</h2><button class="btn" type="button">Done</button>
    </header>
    <div class="rows"><!-- summary tiles as small cards --></div>
    <div class="card ledger"><!-- rows --></div>
  </section>
</div>
```

Open: move focus to the first control; the page under it is `inert`. Close: Done, tap the scrim, Esc, Android Back; restore focus to the opener. The sheet takes the page tint so the cards inside separate.

## Copy rules that keep it calm

- One sentence above a chart says the answer ("Average per day ¥12,700, easing. Most on Nov 6").
- Buttons say what happens ("Add expense", "All the numbers ›"), toasts say what happened ("Expense saved").
- No emoji, no "insights", no badges for status.
