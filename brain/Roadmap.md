---
title: "Roadmap"
tags: ["hati/plan"]
---

# HATI² roadmap (2026-11-09)

What to build next for real use: the owner and Ivy run a household volume ("Bebetime Expenses") and trips ("Baguio 2026") on the web app, the PWA and the APK, with live sync. Made by a structured discussion of every skill, merged by [[hati-plan]], checked by [[hati-goal-judge]]. Invariants hold for every item: no accounts, E2E sync, offline first, integer cents, manga ink, no slop. Back to [[HATI Brain]] · [[Goal]] · [[Team log]]

Facts from the code that shaped the debate (`web/src/app.src.html`):
- The expense form already has a Date field (`#f-date`, ~line 857) and a Note; the "all today" dates come from imported data, not a missing field.
- History is one flat list sorted by date (`timeline()`, ~line 724; `views.history`, 100 rows per page); no search, filter, or month grouping.
- One currency per volume (`CURRENCIES`, ~line 478); types include `household`.
- The volume page today (`views.volume`, ~lines 800–836): hero with "<total> spent" in its meta line, your balance and your own debts; Add expense / Settle up / Share; member chips with balances; "Who pays whom" ledger; Recent (4 rows); "Spending by category" bars with % at the very bottom. Nothing shows spend over time or who paid how much.
- Payments have no method or reference field; volumes already have Undo on delete (`toast(t, {undo})`), expenses do not.
- No duplicate check anywhere. **Owner correction:** the repeated "House of Bagnet" ₱334.13 and the two Chowking entries are real, separate meals on different dates. Same-amount repeats are normal for this couple, so they are not evidence for duplicate detection. What they do show: per-expense dates matter, and the import set every date to today, so the history can't tell the meals apart.
- Open items from the 2026-11-09 handoff: perf regression (patch `brain/WIP/perf-fix-wip.patch`), K3, restore does not re-join sync, real-Worker check never done.

## 1. Proposals (condensed, each skill's voice)

- **hati-plan:** (1) Finish the perf fix first: `web/test.sh` is red, so criterion 1 is unmet and every later round is judged on a flaky base (S). (2) Real two-phone sync check on the live Worker, scripted for the owner (S). (3) Rank everything else by "does the owner hit it this week".
- **hati-code:** (1) Make dates easy to set and fix: quick "Today / Yesterday" choices on the form and a "Set date" for several selected History rows, so the imported "all today" data can be corrected in one pass (M). (2) History search + filter (text, member, category) over the in-memory timeline (M). (3) "Copy as new" on an expense, for rent and bills (S).
- **hati-code-audit:** (1) Restore leaving shared volumes local is a silent fork: two phones then diverge without a word (M). (2) "Backup saved" when the dialog was cancelled is a false promise (S). (3) Any new field must survive `validVolume`/`cleanVolume`/`mergeVolumes` and an older phone, with a data-io check.
- **hati-sync:** (1) Backup carries each volume's `{room, key}`; restore re-attaches and pulls (M). (2) Owner-run real-Worker check (CORS from github.io and appassets) (S). (3) A date fix touches many expenses at once: each gets a fresh `upd` and goes out as one write, so the other phone gets every corrected date and a concurrent edit still merges per expense.
- **hati-money-parity:** (1) Nothing that changes balance math this round. (2) A bulk date change edits `date` and `upd` only, never amounts or splits; "Copy as new" goes through the normal save path. (3) Payment records may gain a method/reference note only; amount logic untouched.
- **hati-design:** (1) History grouped by month with a ruled month heading and the month's total (S). (2) The owner's "at a glance" panel, built from the existing sections (M, see below). (3) No charts, no stat cards (slop risk).
- **hati-lazy-check:** (1) Paid by as a two- or three-way segmented control when the volume has ≤3 members: the couple volume pays "You | Ivy" in one tap, not a picker's two (S). (2) The date must be right without opening the picker in the common cases (today, yesterday); fixing ten wrong dates must not take ten trips through the full form. (3) Search one tap from History, not behind a menu.
- **hati-scenarios:** (1) New scenarios: "household month" (add rent, see this month's total), "restore and re-join", "fix the dates of imported expenses". (2) Re-measure scenario 2 after the payer control (budget 7 → 6). (3) Search scenario: find last month's groceries in ≤3 taps.
- **hati-copy:** (1) Payment method words: Cash, GCash, Bank, Other, plus an optional reference ("GCash ref 1234"). (2) Date words: "Today", "Yesterday", "Pick a date"; bulk action "Set date" with "Dates changed on 6 expenses." as the toast. (3) Honest backup toast: "Backup ready. Check your Downloads." where saving can't be confirmed.
- **hati-a11y:** (1) Payer segmented control is a `radiogroup` with arrow keys and visible labels, not icons. (2) Month headings are real `h2/h3` so screen readers can jump by month. (3) Search field has a label, results count goes to `#status`.
- **hati-motion:** (1) No new animation is needed for any Now item. (2) Month headings are static; the 12-row ink-in cap from the perf patch stays. (3) Selection mode for "Set date" uses the existing press and stamp motion, nothing new.
- **hati-ios:** (1) Re-join after restore matters most on iPhone: the home-screen app's storage can be evicted. (2) Search input 16px, `type="search"`, `enterkeyhint="search"`. (3) Receipt photos: no (storage quota on iOS).
- **hati-android-build:** (1) K3: put the pending save text in a `cacheDir` file before launching the picker, read it back in the result (S). (2) No new Android permissions for any roadmap item. (3) A widget means native code and a second UI; out.
- **hati-ponytail:** (1) Cut: recurring engine, multi-currency, receipts, comments, reminders, widget. (2) "Copy as new" gives 90% of recurring with zero new data shape. (3) Every Now item needs a failing check first.
- **hati-pixel-perfectionist:** (1) Month heading baseline and total align with the row amount column. (2) Payer segments must fit 320 px with long names (ellipsis, equal widths). (3) Search bar the same height as `.input`.
- **hati-slop-rubric:** (1) Per-person stats as donut charts or KPI tiles would score 2 on "dashboard cards"; if ever built, a ruled ledger line per person. (2) Month view stays a ledger. (3) No emoji for payment methods.
- **hati-shots:** add views `history-month`, `history-search`, `expense-payer-seg`, `settle-method` to `VIEWS` when each ships.
- **hati-ship:** ship Now in two rounds: round A = perf + K3 + real-Worker check (no data-shape change); round B = re-join + dates.

### Owner request: an at-a-glance dashboard per volume

Asked for: total spent, who owes whom, spend by category, spend over time (this month vs last for household, per day for trips), top payer. Owners: [[hati-design]] + [[hati-code]].

- **hati-design:** extend, don't duplicate. Total spent and who owes whom already sit in the hero and the "Who pays whom" ledger; by category exists at the bottom. Turn the bottom section into one ruled ink panel, **"At a glance"**, with three ledger blocks: *Spent over time* (household: this month vs last month, two ink bars with the difference in words; trip: one thin ink column per day of the trip), *Who paid* (one line per member: paid, their share, difference; the top payer is simply the first line, marked with a small ink stamp, no trophy), *By category* (the existing bars, top 5 + "Other"). Move it up to right after "Who pays whom"; Recent follows.
- **hati-lazy-check:** the first screen must still read in 3 s as "your balance + Add expense". The panel sits below the fold at 390 px and never pushes Add expense down. Each block answers one question in one line before any bar.
- **hati-slop-rubric:** a grid of KPI tiles with big numbers and icons scores 2 ("every block a card", "dashboard cards"). Allowed: one panel, ruled lines, ink bars in `--fg` with the plate accent only on the current month or today, no donut or line chart, no gradients.
- **hati-pixel-perfectionist:** bars share one left edge and one amount column with the category bars at 320/360/390; per-day columns at 320 px for a 14-day trip are at least 12 px wide, else group by 2 days.
- **hati-a11y:** every chart has its text: bars are `aria-hidden`, each block is a list whose lines carry the numbers ("November ₱18,240.50, ₱2,100.00 more than October"); per-day columns become a `<table>` visually hidden or a "Show days" list; contrast checked in all 13 plates.
- **hati-motion:** no entrance animation on the bars (rule: motion only where it informs); the existing balance roll stays the only number animation.
- **hati-money-parity:** every figure is a sum of integer cents from the same data as balances: "Who paid" diff per member must equal that member's balance before payments; per-month and per-day totals sum to the hero total; percentages are display-only (rounded text, never fed back). Fuzz check added.
- **hati-code / hati-ponytail:** one pure function `glance(v)` returns all numbers in one pass over expenses (no per-render recomputation per block); rendered as static HTML, no chart library. Must not worsen the perf regression: lands after N1, measured in the same 4x-CPU check on a 1,000-expense volume.
- **hati-copy:** heading "At a glance"; lines like "This month ₱18,240.50 · last month ₱16,140.50"; "Ivy paid ₱9,800.00 · her share ₱8,100.00"; no "insights", no "stats".
- **hati-scenarios:** new scenario "How much did we spend this month?" answered from the volume page with 0 taps and a scroll; "Who paid more on the Baguio trip?" same.
- **hati-sync / hati-ios:** read-only, no data shape change; nothing to sync. 16 px minimum text holds.
- **Settled (hati-plan):** **Next #1 (X1)**, not Now: spend over time is meaningless until the dates are right (N5) and it adds render work on top of the open perf regression (N1). It extends the existing sections rather than adding a second dashboard.

## 2. Challenges and how they were settled

- **Multi-currency trips** (proposed for future Asia trips) — *money-parity* vetoes: rates need a network source beyond `SYNC_BASE`, rounding across currencies breaks parity with `CalculateDebtsUseCase.kt`. *Plan:* **no**. One currency per volume; a foreign leg is its own volume.
- **Recurring rent and bills** — *sync*: two phones would both generate March rent offline (different ids, merge keeps both, rent counted twice). *Ponytail*: an engine is L for a monthly 5-tap job. *Plan:* **later**; "Copy as new" in Next. Revisit only with deterministic ids (template id + month).
- **Receipt photos** — *sync*: blob cap 512 KB per volume, one photo breaks sync; *ios*: localStorage quota. *Plan:* **no**.
- **Keys in backups** — *code-audit*: a backup file then grants write access to the room. *Sync*: anyone holding the backup already has all the data, and the invite link (already in chat history) grants the same power. *Plan:* sync upheld; the backup sheet says "This file can open your shared volumes. Keep it private." (copy owns the words).
- **Payer quick-switch vs the settled "Paid by stays a native select" (loop 3 round 2)** — *lazy-check* brings new evidence: the real volumes have 2 members, where chips can't grow. *a11y* requires a radiogroup. *Plan:* reopened on that evidence: segmented for ≤3 members, select above that. Next, not Now (a nicety, not a loss).
- **Duplicate detection** — first proposed by *code* from the owner's repeated Bagnet and Chowking entries; the owner corrected that they are real, separate meals. *Lazy-check*: any check on save that interrupts a normal save fails "goal in one tap". *Settled:* **out of Now**; at most later a gentle same-day hint that never blocks Save (same amount, payer and description on the same date only), and only if double entries are actually reported. The real gap is dates (N5).
- **Monthly view** — *design* wanted a separate month screen; *ponytail* and *slop-rubric*: grouping History by month is the same information with no new view. *Settled:* month headings + totals in History (X2); "this month vs last" lives in the At a glance panel (X1).
- **Per-expense dates** — *ponytail* first argued the Date field already exists, so no new work. *Code* and *lazy-check*, with the owner's correction: the imported data has every date set to today, so fixing it one by one through the full form is many taps for each wrong date, and the month view (X2) and At a glance (X1) would put everything in this month. *Settled:* weighted up into **Now** (N5): quick Today/Yesterday on the form plus a "Set date" for several selected History rows. *Money-parity* condition: it changes `date` and `upd` only.
- **Where At a glance sits** — the owner's request reads "at the top"; *lazy-check* and *design* object that the top of a volume is the balance and Add expense, and a dashboard above them fails the 3-second test. *Settled:* the hero already carries total spent and your debts (the true "top"); the panel follows "Who pays whom", reachable in one scroll, and replaces the bottom "Spending by category" so nothing is shown twice. Revisit if the owner says it is too far down.
- **Search placement** — *lazy-check* vs *design* (menu vs visible field): *settled* a search field at the top of History, shown when the volume has more than 20 items.

## 3. Roadmap

### Now (next 1–2 rounds)

| # | Item | Owner | Size | Acceptance checks | Risks |
|---|---|---|---|---|---|
| N1 | **Finish the perf regression** (apply `brain/WIP/perf-fix-wip.patch`: ink marks on the first 12 rows, `content-visibility:auto` after row 32) | hati-motion with hati-code | S | Both "M-perf 4x CPU" checks pass three idle runs in a row; `web/test.sh` 0 FAIL; motion frames unchanged; then delete `brain/WIP/` | Content-visibility clips focus rings (patch keeps `:focus-within` rows drawn); flaky timing when agents run in parallel |
| N2 | **Real two-phone sync check** on the live Worker: owner script (Pages link on iPhone + APK on Android, add, go offline, reconnect, delete) | hati-sync (owner runs it) | S | Owner reports each step; any CORS/Origin failure gets a repro and a fix; result logged in [[Team log]] | Worker CORS for `appassets.androidplatform.net`; room TTL |
| N3 | **K3: APK save survives process death** (pending text in a `cacheDir` file, cleared after write or cancel) | hati-android-build | S | Instrumented or manual "Don't keep activities" run: save → picker → kill → pick → file has the text and the toast shows; CI green | Stale cache file if the picker is abandoned (clear on next launch) |
| N4 | **Restore re-joins shared volumes**: backup carries `sync:{room,key}` per volume; restore re-attaches and pulls/merges | hati-sync with hati-code, hati-copy | M | data-io: backup → wipe → restore → the volume reports "Synced" and pulls a change made meanwhile on the other context; expired room re-uploads; old backups (no keys) still restore; backup sheet privacy line | Backup becomes sensitive; a wrong key in a file must leave local data untouched (existing S6-7 path) |
| N5 | **Dates that are right and easy to fix**: "Today / Yesterday / Pick a date" on the expense form; in History, a "Set date" mode to select several rows and give them one date (for the imported "all today" data); each row shows its date | hati-code with hati-lazy-check, hati-copy, hati-a11y | M | e2e: add an expense dated Yesterday in one tap after the amount; select 6 imported rows → Set date → reload, backup and the other synced context show the new dates; amounts and splits byte-identical; selection mode is keyboard and screen-reader operable; scenario "fix imported dates" budgeted | A mis-tap changes many dates: toast with Undo; a concurrent edit on the other phone merges per expense by `upd` |

Ship as two rounds: A = N1, N2, N3 (no data-shape change); B = N4, N5.

### Next

| # | Item | Owner | Size | Acceptance checks | Risks |
|---|---|---|---|---|---|
| X1 | **At a glance on every volume** (owner request): one ruled ink panel after "Who pays whom": spend over time (household: this month vs last; trip: per day), who paid (paid / share / difference, top payer first), by category as ONE stacked category strip with a legend in the app's category order (owner picked the "alt" layout of the 2026-11-09 mock, `mockups/dashboard.html?v=alt`, kept out of git because it holds real data) | hati-design + hati-code, with hati-a11y, hati-money-parity | M | First screen at 360/390 still shows balance and Add expense without scrolling; per-month, per-day and per-member figures each sum to the hero total in cents (fuzz); each member's paid−share equals their balance before payments; every bar has its numbers in text (axe clean, screen-reader list); slop rubric 0 at 360/390 both themes, no tiles; pixel.js clean at 320; 4x-CPU volume open on 1,000 expenses no slower than before N1's baseline + 5 ms | Becomes a KPI-tile grid (slop); render cost on big volumes; wrong imported dates (needs N5 first) |
| X2 | **History by month**: ruled month headings with the month total | hati-design with hati-code | S | e2e: headings in order, totals = sum of that month's expenses in cents; `h2`/`h3` semantics; slop rubric 0; pixel.js clean at 320/360/390 | Wrong imported dates land in the wrong month until fixed (N5) |
| X3 | **Search and filter in History** (text in description/note, member, category; field shown past 20 items) | hati-code with hati-a11y | M | e2e: "groceries" finds by category and text; count announced via `#status`; 16px input; clearing restores the list; 1,000-row search under 50 ms at 4x CPU | Render cost on big lists (reuse the paged list) |
| X4 | **Paid by quick-switch** for ≤3 members (segmented radiogroup; select above 3) | hati-lazy-check with hati-design, hati-a11y | S | Scenario 2 budget 6 taps on a 2-member volume; arrow keys move the choice; long names ellipsize at 320 px | Reopens a settled decision: logged with the new evidence |
| X5 | **Payment method and reference** on settle-up (Cash, GCash, Bank, Other + optional ref, max 40 chars) | hati-copy with hati-code, hati-sync | S | Field optional, survives reload, backup, merge and an older phone's record; amount math untouched (money-parity fuzz unchanged) | An older phone that edits the payment drops the field: both phones update first |
| X6 | **Copy as new** on an expense (prefills the form, date today), for rent and bills; Undo toast for expense and payment delete | hati-code with hati-lazy-check | S | e2e: copy → Save gives a new id with today's date, original untouched; delete → Undo restores it and syncs the restore | Undo after a sync push must re-add via the normal write, not resurrect the tombstone silently |

### Later / no

- **Recurring expenses (later, L):** two offline phones would both generate the same bill; Copy as new (X5) covers it. Only with deterministic ids per template and month.
- **Per-person stats:** now the "Who paid" block of X1; no separate screen.
- **CSV export (later, S):** nobody has asked to open the data in a spreadsheet; JSON backup covers safety. Cheap when asked.
- **Duplicate hint (later, maybe never):** owner says same-amount repeats are normal. If double entries are ever reported: a gentle same-day hint that never blocks Save, never auto-deletes.
- **Custom categories (later):** the 12 were settled on 2026-11-09; reopen with real "Other" usage data.
- **Honest backup toast (later, S copy):** "Backup ready. Check your Downloads." where saving can't be confirmed.
- **Multi-currency (no):** breaks money parity, needs a rate source on the network.
- **Receipt photo (no):** 512 KB sync blob cap and iOS storage quota.
- **Reminders (no):** need a push server and identity; against no accounts.
- **Home-screen widget (no):** impossible on iOS PWA; native-only work breaks G1 parity.
- **Comment thread (no):** the Note field covers it; per-message merge adds conflict cases for little use.

## 4. Goal judge verdict

- **G1 (both phones):** served. N3 closes the last known Android data-path gap, N2 is the first real-phone proof of live sync, N4 protects iPhone users whose storage can be evicted. Not met until N2 is reported by the owner.
- **G2 (real use):** served. N4 stops a silent fork after a restore and N5 makes the dates true, which At a glance (X1), the month view (X2) and every "when did we pay that" question need; X1 answers the owner's "where does our money go" at a glance and X2–X6 are the household's everyday jobs. Nothing touches balance math.
- **G3 (not slop):** served by what was cut: no chart library, KPI tiles, donuts or emoji methods; the owner's dashboard is one ruled ink panel built from existing sections. X1 (the dashboard, highest slop risk), X2 and X4 must pass the slop rubric and pixel.js before ship.
- **Criterion 1 (`web/test.sh` green) is unmet today:** N1 goes first, no exceptions.

## Depth pass (2026-11-09)

Owner: "What features to add more? Goal is to deepen the app, not broaden." Depth = the existing jobs done better: add an expense, see who owes whom, settle up, share/sync a volume, browse history, back up. No new feature areas, screens or integrations. Every skill proposed and challenged (one agent voicing them, per the owner's rule when usage is tight); [[hati-plan]] merged.

Facts from the code (`web/src/app.src.html`) behind the debate:
- New expense (`formFrom`, ~l.940): description empty, payer always `meId`, split always Equal, category = this volume's last category (`lastCat`). Nothing learns from "Chowking" or "House of Bagnet" repeating in Bebetime. A repeat meal Ivy paid: amount (1) + description (1) + category (1 unless last) + payer select (2) + Save (1) = 6 taps.
- Settle-up sheet (`settleSheet`, ~l.1217): prefilled from the first suggested debt and the amount is editable, so partial payments already work in 2 taps. But the payment's date is always `today()` with no field, even on edit, and there is no method or reference.
- Undo exists only for volume delete (`toast(t,{undo})`, ~l.1123). Deleting or editing an expense or payment is final.
- Remote changes (`syncRun` merge, ~l.1590) only ink-mark new rows (`FX.fresh`); an edit from the other phone changes a number silently, and nothing says who changed it. The status tag says "Synced"/"Offline — will sync" with no time and no count of waiting changes.
- `splitEqual` (~l.600) gives the extra centavo to members in list order, so on odd-centavo meals (₱334.13 → 167.07/167.06) the same person always carries it, and the preview doesn't say so.
- `balances()` is a sum the user never sees: "You owe Ivy ₱1,240.55" has no way to show its parts.
- Backup: no record of when the last one was made; the "Backup saved" toast can't confirm a save on the web (Later item).

### Proposals and challenges (condensed)

- **hati-lazy-check:** repeat entries are this couple's daily job; one tap on a past description should fill it. Challenged *ponytail* "remember last payer": for a couple the payer alternates, so remembering is wrong half the time; X4's one-tap payer switch fixes it without guessing. *Settled:* payer stays "you", X4 folded into D1.
- **hati-code:** suggestions come from this volume's own expenses (most recent per normalized description, top 3 matches by prefix then frequency); no new data. Picking one fills description, category, and split type/weights when the same members are included.
- **hati-money-parity:** a suggestion must never fill the amount (same-amount repeats are real, but a silent ₱334.13 on a ₱412 meal is a wrong balance nobody notices). *Lazy-check* objected (Bagnet repeats exactly); *settled:* the last amount shows as a tappable hint "Last time ₱334.13" beside the field, one extra tap, never auto-filled. Weights copied, cents recomputed by `splitWeighted`.
- **hati-a11y / hati-ios:** no `<datalist>` (iOS Safari renders it poorly, VoiceOver reads it inconsistently) and no full combobox; up to 3 plain buttons under the field ("Chowking · Eating out"), results count to `#status`, 44 px targets.
- **hati-sync:** D1, D3, D4, D6, D7, D8 change no data shape. D2 adds optional `method`, `ref` and an editable `date` on payments; D5 adds optional `by` (member id of the writing phone's `meId`) on expenses and payments. Both survive `cleanVolume`/`mergeVolumes` with the winning record; an older phone drops them, which only loses a label. `by` is a label, not proof (no accounts) — *code-audit* accepted that.
- **hati-code-audit:** Undo of a delete must re-add through the normal write with a fresh `upd` and clear the tombstone, or the other phone keeps it deleted; undo of an edit writes the previous fields with a fresh `upd`. Repro required both ways across two contexts.
- **hati-money-parity:** "why this balance" lines are the same integer-cent sums as `balances()`: paid − share + payments sent − payments received = balance, checked by fuzz. The centavo: rotate the extra cent to the payer when the payer is included, else list order. Debt math (`CalculateDebtsUseCase.kt`) reads stored splits, so parity is unchanged; splits still sum exactly.
- **hati-design / hati-slop-rubric:** D4 is a sheet with a ruled ledger, not a new screen and not tiles; D5 is words in the toast and a small "edited by Ivy" in the row meta, no badges or avatars stacks.
- **hati-copy:** "Last time ₱334.13"; "Ivy added Chowking · ₱412.00" / "Ivy changed 2 expenses"; "Synced 2 min ago" / "3 changes waiting"; "+₱0.01 goes to Ivy"; "Last backup 12 days ago"; "Backup ready. Check your Downloads." where saving can't be confirmed.
- **hati-motion:** no new motion. Haptics (*proposed*) rejected: `navigator.vibrate` doesn't exist on iOS, so it splits G1 behavior; the existing press and stamp carry feedback.
- **hati-ponytail:** cut a custom ₱ keypad (the native decimal keypad already works, a custom one is slop risk and an a11y cost); cut arithmetic in the amount field until asked; "Copy as new" (X6) is covered by D1 suggestions. Every D item needs a failing check first.
- **hati-scenarios:** new budgets: "repeat Chowking, Ivy paid" ≤4 taps (from 6); "record yesterday's GCash payment" ≤4; "undo a wrong delete" 1 tap; "why do I owe Ivy" 1 tap from the volume page.
- **hati-pixel-perfectionist:** suggestion buttons share the input's left edge and height; the "Last time" hint sits on the amount baseline; no layout shift when hints appear (reserved line).
- **hati-ios:** the install path itself has no open defect (banner + standalone note exist); its depth is D8 (storage eviction makes backup trust matter most on iPhone). No separate item.
- **hati-a11y:** accessibility depth is an acceptance line on every item below, not its own item.

### Depth backlog (ranked)

| # | Item | Owner | Size | Acceptance checks | Slot |
|---|---|---|---|---|---|
| D1 | **Add-expense learns from the volume**: up to 3 description suggestions from this volume's past expenses; a tap fills description, category and split type/weights; "Last time ₱…" hint fills the amount only when tapped; payer one-tap switch for ≤3 members (absorbs X4) | hati-code with hati-lazy-check, hati-a11y, hati-money-parity | M | Scenario "repeat Chowking, Ivy paid" ≤4 taps; amount never prefilled without a tap (e2e); copied weights give splits summing to the amount (fuzz); buttons reach by keyboard, count via `#status`; no new stored field; 16 px inputs | Next #1 (after Now round B) |
| D2 | **Undo on every change**: delete and edit of expenses and payments get the toast Undo (absorbs X6's undo half) | hati-code with hati-sync, hati-code-audit | S | Delete → Undo restores and the other context gets it back (tombstone cleared, fresh `upd`); edit → Undo restores previous fields on both contexts; balances byte-identical to before | Next #2 |
| D3 | **Payments recorded as they happened**: date field on the settle sheet (Today/Yesterday as in N5), optional method Cash/GCash/Bank/Other + ref ≤40 chars (absorbs X5) | hati-code with hati-copy, hati-sync | S | Edit keeps/changes date; fields survive reload, backup, merge, older-phone record; amount math untouched (money-parity fuzz unchanged); scenario "yesterday's GCash payment" ≤4 taps | Next #3 |
| D4 | **Why this balance**: tap your balance or a member chip → sheet with that member's ledger: paid, share, payments sent, received = balance, then the entries behind it (newest first, 20 then "Show more") | hati-design + hati-code, with hati-money-parity, hati-a11y | M | Lines sum to `balances()` in cents for random volumes (fuzz); reuses X1's `glance(v)` pass; heading + list semantics; slop 0, ledger not tiles; 1 tap from volume page | Next #4 (before X1, shares its function) |
| D5 | **Changes from the other phone in words**: arrival toast names who and what ("Ivy added Chowking · ₱412.00", "Ivy changed 2 expenses"); History row meta shows "edited by Ivy" when `by` ≠ you; optional `by` field | hati-sync with hati-copy, hati-code | M | Two-context e2e: add/edit/delete on B → A's toast names B's member and the item; older record without `by` shows no label; `cleanVolume` rejects a `by` that isn't a member; toast via `#status` | Next, after X1 |
| D6 | **Sync status you can trust**: tag shows "Synced 2 min ago" / "3 changes waiting" / "Offline — 3 changes will sync" | hati-sync with hati-copy | S | Offline e2e: count rises per change, falls to 0 on reconnect; time updates without re-render jank; no new stored data | Next, after D5 |
| D7 | **The extra centavo, fair and shown**: equal split gives the odd cent to the payer when included, else list order; preview says "+₱0.01 goes to Ivy" | hati-money-parity with hati-code, hati-copy | S | Splits sum exactly (fuzz 10k); preview text matches stored splits; old expenses untouched; Android debt parity test unchanged | Next #1, ships with D1 |
| D8 | **Backup you can trust**: "Last backup N days ago" in Settings (this phone only, in `settings`), honest toast where saving can't be confirmed (promoted from Later) | hati-copy with hati-code, hati-ios | S | Line updates after a backup, survives reload, not synced, not in the backup; toast wording per platform; restore path unchanged | With N4 (round B) |

### Order and what is demoted

- **Now is unchanged** (N1–N5): perf, real two-phone check, K3, restore re-join, dates are all depth already.
- **Next, new order:** D8 rides with N4 → D1 (+D7) → D2 → D3 → D4 → X2 → X1 → D5 → D6.
- **Kept in Next:** X2 History by month (depth of browsing history). X1 At a glance stays (owner request, settled layout) but after D4, which builds the shared `glance(v)` function; its "spent over time" block is the one breadth-leaning part and must stay one ruled line pair, no chart growth.
- **Folded in:** X4 → D1, X5 → D3, X6 Undo → D2.
- **Demoted as breadth (to Later):** X6 "Copy as new" (a new action; D1 suggestions cover rent and bills), X3 Search and filter (a new control and index; for a 2-member volume month headings plus D4's per-member ledger answer "when did we pay that"; reopen when a volume passes ~300 items).
- **Rejected in this pass:** custom ₱ keypad, amount arithmetic, haptics, remember-last-payer, `<datalist>`.

**Judge (hati-goal-judge):** G2 served most (D1–D4 hit the household's daily jobs, nothing new to learn); G1 kept (no iOS-only or Android-only behavior, haptics cut for that reason); G3 kept (sheets and ledgers, no tiles, no new motion).
