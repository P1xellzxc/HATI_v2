# Charts in matte glass

## Form

- **Spending / activity over time:** one line per category for the top 3 + "Other" (more fold into Other), and the **moving average of the total of all categories** as the one heavier, dashed line in `--fg` (readable without colour). Trips/short ranges: per day, 3-day average. Households/long ranges: per month, 3-month average. More than 31 days of daily data → switch to months.
- **One axis** in the data's unit. Never a second y-axis.
- **Lines:** categories 1.25px, average 2px dashed 5/4, round caps; end dots 4px with a 1.5px surface ring. Grid 3–4 ticks, 1px, recessive. Labels from text tokens, never the series colour.

## Colour (validate, don't eyeball)

Default categorical slots (fixed order, colour follows the category, never its rank):

| Slot | Light | Dark |
|---|---|---|
| 1 | `#2a78d6` | `#3987e5` |
| 2 | `#eb6834` | `#d95926` |
| 3 | `#1baf7a` | `#199e70` |
| 4 | `#eda100` | `#c98500` |

Measured on glass surfaces (`#eef3fc` light, `#2a2930` dark): colour-blind adjacent ΔE ≥ 9.1 / 8.4, normal-vision ≥ 19.3 — pass. In light mode three marks are under 3:1 against the glass, so **identity is never colour alone**: legend chips with each total, tap values, and the numbers sheet. Re-validate when you change the palette.

## Layout

1. Title + range + one-sentence answer on the page.
2. **Chart slab** (its own glass card): the SVG only.
3. **Key slab:** legend chips in balanced rows of 2 (swatch, name, short total like `¥65.2k`; tap = hide/show, never all hidden), the average as a full-width chip, then an **"All the numbers ›"** button.
4. Tap or drag on the chart: a crosshair, dots on every line, and a glass tooltip with that point's values (flips side at the middle).

## "All the numbers" sheet

- Summary tiles: total, per day/month, busiest day/month.
- By category: total, share %, biggest single day.
- Each day/month table: every category, the day total, the moving average, and a totals row. `th scope="col"` and `th scope="row"`.
- **Pin the first column** when the table scrolls sideways:

```css
.sheet table :is(th, td):first-child { position: sticky; left: 0; z-index: 1; background: var(--g-top); box-shadow: 6px 0 6px -6px var(--g-shadow); }
.sheet table thead th:first-child { z-index: 2; }
```

## Money and numbers

- Compute from integer minor units (cents); categories sum to the period total; averages and percentages are display only.
- Short axis labels (`¥10k`), full values in tooltips, chips and the sheet.
