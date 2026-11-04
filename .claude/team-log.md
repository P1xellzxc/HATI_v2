# HATI² team log

Shared notes between the persona agents (planner, critic, designer, coder, code auditor, design auditor). Newest round at the bottom. Read it before you start; append your handoff when you finish.

## Decisions (settled; don't reopen without new evidence)

- The web app (`web/src/app.src.html` → `docs/app/`) is the product for both Android and iOS users; iOS installs via Add to Home Screen. No native iOS build.
- (Owner) Decisions inside the project are delegated to the team: the owning skill decides against the goal, records it here, and carries on. Only actions outside the repo (Pages, sharing, accounts) go to the owner.
- (hati-android-build) Restored the last known-good Android dependency set from before the unbuilt bulk upgrade in 4000388 (Hilt 2.52, Room 2.6.1, Compose BOM 2025.01.01, core-ktx 1.15.0, lifecycle 2.8.7, activity-compose 1.10.0, navigation-compose 2.8.5, datastore 1.1.1, test libs to their previous versions), keeping AGP 8.7.2, Kotlin 2.0.21, compileSdk 35. Evidence: pre-upgrade build_result.txt shows BUILD SUCCESSFUL. Upgrades resume one coupled group at a time, each confirmed by CI.
- Kotlin/Android code stays out of the web loop's scope; the hati-android-build skill owns it.
- Design language is manga ink on paper (see `web/README.md`). Glassmorphism and blurred gradient blobs were removed as AI-slop tells and stay out.
- Every persona decides from the goal in `web/README.md` (G1 both phones, G2 real use, G3 not AI slop) and the lazy-user check is part of every design step.
- Money is integer cents; debt math mirrors `CalculateDebtsUseCase.kt`.
- (Owner, round 1) Manga acetate glass is approved to explore: translucent floating layers only (top bar, nav, sheets) with screentone and 2px ink edges, never on scrolling content, never over gradient blobs. Revert if `hati-slop-rubric` or `hati-lazy-check` flags it. This updates the earlier "no glass" decision.
- (Owner) Every agent applies the "deeper version" prompt to its own assignment first and writes the four answers plus the rewritten assignment in its log entry.
- (Owner) A ponytail review runs after every change (code especially) and a ponytail audit of web/ runs before each ship; a pixel perfectionist audits alignment, spacing and balance every round.
- (Owner, round 1) New team members from round 2: iOS tester, scenario tester, goal judge. New shared skills: hati-shots, hati-ios, hati-scenarios, hati-money-parity, hati-copy, hati-a11y, hati-ship, hati-lazy-check, hati-slop-rubric.
- Round 1: importing a volume you already have must never silently drop local entries; the default import path merges by id (expenses, settlements, members), with deletions carried as tombstones. "Replace" is not the default.
- Round 1: share stays text/file only (no server, no network). On phones the primary share action is the native share sheet (navigator.share) with copy as fallback; raw JSON is not the first thing a user sees.

## How to write an entry

```
### Round N · <role>
- Goal moved: G1/G2/G3 and how
- Did / found: ...
- For @planner / @critic / @designer / @coder / @code-auditor / @design-auditor: questions or requests, one per line
- Disagree: what and why (the planner settles it against the goal and records it under Decisions)
```

## Rounds

### Round 1 · planner
- Goal moved: planning only. Ranked for G2 (real two-phone use, no data loss) first, then G3/lazy-user, then G1.
- Evidence: `web/test.sh` 36/36 PASS (raw: scratchpad/loop/r1/plan/test/e2e.txt). Screenshots of hub, volume, expense, settle, menu, share, import, settings at 360/390, light/dark: /tmp/claude-0/-home-user-HATI-v2/aa252cc6-0787-543f-8ff4-5d833c851890/scratchpad/loop/r1/plan/{1hub..8settings}-{light,dark}-{360,390}.png
- Repro (blocker, G2): phone A shares Tokyo Trip, phone B imports as Mira and adds "FromB"; A adds "FromA"; A imports B's code with the default "Replace my copy" -> A's list is Ramen|Shinkansen|Shibuya|teamLab|FromB. "FromA" is gone, no warning. `importSheet()` mode "replace" in web/src/app.src.html (~line 826).
- Found: no way to rename or remove a member (only `addMember` action exists); a typo or wrong person is permanent. Share sheet (`shareSheet()`) shows a raw JSON textarea as the main content; only "Copy code". Settings palette list is 13 full-width tinted cards (~1950 CSS px at 360), pushing Data to the bottom.
- Plan (max 5, ranked):
  1. [hati-code, G2] Merge on import. `importSheet()` + `cleanVolume()`/`validVolume()`: default option "Merge with my copy" when the id exists; union expenses/settlements/members by id; on same id keep the entry with the newer `upd` (ms timestamp, set on create/edit; missing = 0, incoming wins ties); deletions recorded in `v.deleted` (array of ids, validated with ID_RE, cap 20000) and applied on merge; keep local `meId`. Accept: Given A and B diverge as in the repro, When A imports B's code with defaults, Then A has both FromA and FromB, an expense B deleted is gone on A, and balances sum to zero.
  2. [hati-code, G2] Edit members. Tap a member chip -> sheet with Name (rename, 1..40 chars, trimmed) and "Remove" enabled only when the member has no expenses, splits or payments (otherwise a sentence saying why). Cannot remove yourself (`meId`). Accept: Given Mira, When renamed to "Mia", Then volume, debts and history show Mia and old name nowhere; Given a member with an expense, Then Remove is disabled with the reason visible; Given a new unused member, When removed, Then members count drops and zero-sum holds.
  3. [hati-code, G1/G2] Native share + device backup. `shareSheet()`: primary button "Send…" calls `navigator.share({title, text: code})` when available, else copies; Copy stays. Settings > Data: "Save backup" downloads `hati2-backup-YYYY-MM-DD.json` with all volumes; Import accepts a backup file (each volume validated, then merged per item 1). Call `navigator.storage.persist?.()` once after first save (ignore result). Accept: with `navigator.share` stubbed, tapping Send passes text that `importSheet` accepts; Playwright download event yields a file whose re-import on a fresh context restores all volumes.
  4. [hati-design, G3/lazy-user] Share sheet reads as a friendly step, not a dev tool: summary line (title · members · expenses), the Send/Copy actions first, the code collapsed under a "Show code" disclosure, and the import sheet's placeholder no longer shows `{"app":"hati2-mock"…}`. Rubric: a cold viewer of share-*.png can say what to do in 3 seconds; no raw JSON visible without a tap.
  5. [hati-design, G3] Compact palette picker in Settings: swatch grid (e.g. 3-4 per row at 360) keeping role=radio, names readable, check on current. Accept: settings page full height at 360x780 <= 1300 CSS px; all 13 palettes still selectable by name (`getByRole('radio',{name:/Persimmon/i})`); no horizontal scroll at 320.
- Not doing: undo for "Delete payment" (one explicit tap inside an edit sheet; lower harm than items 1-3); live sync/QR (needs network or camera libs); member avatar letter "A" next to "You" (cosmetic); Kotlin.
- For @coder: items 1-3, in that order; item 1 first because item 3's backup import reuses its merge. Add the repro above to `web/tests/e2e.js` as a two-context test.
- For @designer: items 4-5. Item 4 shares `shareSheet()` with item 3; coordinate with @coder (coder owns the buttons' behavior, designer owns layout/copy).
- For @code-auditor: try to lose data through merge (edit vs delete on two phones, member removed on one side while used on the other, crafted `deleted`/`upd` fields).
- For @design-auditor: re-score share, import and settings screens at 360/390 both themes.
- For @critic: challenge whether tombstone merge is the smallest fix that stops the data loss.
- Disagree: none yet (first round).

### Round 1 · critic
- Goal moved: none (review only). All five items are real problems, checked against the source. The amendments below make them safe to ship and testable.
- @planner, answering whether a tombstone merge is the smallest fix: yes. A union-only merge would bring deleted entries back, and Replace loses data. It is only safe with these guards:
  - R1-1: `cleanVolume()` copies only the fields it knows, so it would drop `upd` and `deleted`. `normalize()` runs it on every load, so both would vanish after a reload. Both functions must keep the two fields, and the test must reload after merging.
  - R1-1: `normalize()` silently drops any volume that fails `validVolume()`, which is a second data-loss path. Run `validVolume()` on the merged result before saving it. If it fails, do not change anything and show a message.
  - R1-1: Set `upd` in `submitExpense`, in the settlement edit (`Object.assign(existing,…)`) and in the member edit (R1-2). Push a tombstone in `deleteExpense` and `deleteSettle`. When one phone edits and the other deletes, the delete wins; the test should prove it.
  - R1-1: Accept `upd` only if it is an integer from 0 to Date.now()+1 day. Otherwise a code with a far-future date would win every merge forever.
  - R1-1: When merging, skip the "Which member are you?" picker and keep the local `meId`.
  - R1-1: Before importing, show what the merge will do, for example "Adds 2 · updates 1 · removes 1".
  - R1-1: Old codes without `upd` or `deleted` must still import.
  - R1-2: Member tombstones must be ignored when a merged expense or settlement still uses that member. Otherwise the merged volume fails validation.
  - R1-2: A member counts as "used" if their id is a key in `splits` (even with 0 cents, which exact and shares splits can produce), is in `inputs`, is `paidBy`, or is a settlement's `from` or `to`. A 0-cent split key left behind makes `validVolume` fail, and the whole volume is dropped on the next load.
  - R1-2: A rename must reuse `memberSheet`'s rules: no duplicate names (case-insensitive) and not "You".
  - R1-2: `memberChip` is currently a `<span>`. It must become a real button.
- @coder: share-sheet details.
  - `navigator.share` throws `AbortError` when the user cancels. That must not trigger the copy fallback or show an error.
  - Do not await anything before calling `share()`, or the browser loses the user gesture.
  - A backup has no "which member" step. Each volume keeps its own `meId`.
  - Accept codes pasted with chat text around them by parsing from the first `{` to the last `}`.
- @designer: with the code collapsed under "Show code", the copy fallback (`ta.select()`) has to open the disclosure first.
  - Measure the 1300px Settings height after R1-3 adds the backup rows.
  - The arrow keys on the palette radios must still move through all 13 in the grid.
  - The current palette must be marked by more than colour.
- Disagree, @planner: "Delete payment" without a confirm was harmless when it only affected one phone. With tombstones the delete now spreads to every phone that merges. Use the existing `confirmSheet` (one line) or move it out of not_doing.

### Round 1 · designer
- Goal moved: G3 and lazy-user (share and import sheets read as a plain next step, not a dev tool; Settings shrinks from ~1950 to 1186 CSS px at 360x780 so Data is reachable) and G1 (Send… opens the phone's own share sheet).
- Did, all in `web/src/app.src.html`:
  - R1-4 `shareSheet()`: summary block (title in display face, "3 members · 4 expenses · 1 payment", accent ink edge), one line of plain instructions, then the actions. Send… (primary) appears only when `navigator.share` exists; otherwise Copy code is the full-width primary, so no two buttons do the same thing on desktop. The code sits in a closed `<details id="s-codebox">` "Show code". Done is a quiet button at the end. All values pass `esc()`/`plural()`.
  - R1-4 `importSheet()`: help line ("Someone shared a volume with you? Paste the code they sent, then tap Check code."), label "Share code", placeholder "Long-press here and tap Paste" (no JSON). The native "Choose File / No file chosen" input is visually hidden behind an "Open file" button-label (`.filepick`; focus ring shows on the label).
  - R1-5 `views.settings()`: `.pals` grid of printed swatch plates (auto-fill, min 88px: 3 per row at 320-390), each keeps role=radio, roving tabindex, the real name (no longer uppercased) and Wada number; the current one has an ink offset shadow in the theme's rule colour (it vanished in dark before) plus a ✓ badge. Appearance and Color theme are merged into one panel and the Data buttons stay side by side at 360 (`.btns.pair`).
  - Every sheet: removed the yellow focus box drawn around the sheet title when it gets programmatic focus (`.sheet h2:focus-visible`), which looked like a selection bug in every sheet screenshot.
- Behaviour I touched, @coder (you own it, change freely): to avoid a dead Send button I added `case "sendCode"` (calls `navigator.share` first with no await before it, AbortError is a silent no-op, other errors and no-share fall back to copy) and `copyShareCode()` (clipboard; on failure it opens `#s-codebox`, focuses and selects the code). Merge/backup work in `importSheet()` is untouched; keep `#s-pick`, `#s-go`, `#s-paste`, `#s-file` ids. If you add backup rows to Settings > Data, put them in the same `.btns.pair` row or one more row; the budget left under 1300px is ~110px.
- Tests (`web/tests/e2e.js`, block "R1-4/R1-5 (design)"): share summary, no JSON visible before Show code, Show code reveals, clipboard denied opens and selects the code, Send passes `#s-code` text to a stubbed `navigator.share` without copying, AbortError is silent, import placeholder has no JSON, Open file is labeled, Settings ≤1300px at 360 (1186), all 13 radios selectable by name, check mark on the current one, ArrowRight walks all 13 with one tab stop, no sideways scroll at 320. `PORT=8771 web/test.sh` 50/50 PASS (scratchpad/loop/r1/design/test/e2e.txt).
- Contrast: `web/tests/contrast.js` no longer skips `.pal`; swatch text is measured against each palette's own paper. Lowest in Settings: Spring "No. 347" 4.63:1, both themes. `web/tests/shots.js` gains `sheet-share-phone` (navigator.share stubbed) and `sheet-share-code` (disclosure open).
- Screenshots: before /tmp/claude-0/-home-user-HATI-v2/aa252cc6-0787-543f-8ff4-5d833c851890/scratchpad/loop/r1/design/before/, after .../design/after/ (classic-{light,dark}-{360,390}-{sheet-share,sheet-share-phone,sheet-share-code,sheet-import,settings}.png; index.html contact sheet), full-page Settings .../design/settings-full-{light,dark}.png.
- Lazy-user check on after shots: share sheet says what (summary), what to do (one sentence) and the one tap (Send/Copy) above the fold at 360 in both themes; import has no JSON and no unstyled file control; Settings shows Appearance and all palettes in about one and a half screens. Tap targets ≥44px, inputs 16px.
- For @design-auditor: contrast.js still FAILs, but only on a pre-existing issue outside my items: `.hero .meta` ("Vol. 01 · Travel · …") on the volume view in light mode is 3.77–4.4:1 in 11 palettes (same numbers before my change). Please score it; one-line fix is a darker `--sub` mix on `--hero`.
- For @planner: put the `.hero .meta` contrast fix in round 2 (or let me take it) so contrast.js can be gated in `web/test.sh`.
- For @critic: applied the R1-4 amendment (disclosure opens before select; summary escaped) and R1-5 (height measured with scrollHeight, 4.5:1 on own paper, check + aria-checked, arrow-key test). The 1300px measure was taken before R1-3's backup rows; headroom is left for them.
- Disagree: none.

### Round 1 · coder
- Goal moved: G2. Importing a copy of a volume you already have now merges it, so neither phone loses entries. Members can be renamed and removed safely, and a backup file guards against cleared storage. G1: Send… hands the code to the phone's own share sheet, and the text it sends imports on another phone.
- Did, all in `web/src/app.src.html`:
  - R1-1 merge. New `mergeVolumes(local, incoming)` unions members, expenses and settlements by id. On the same id the newer `upd` wins and incoming wins ties. A tombstone in `v.deleted` beats any edit. The local `meId` is kept. Volume title, type and currency follow the copy with the newer volume `upd`, so a currency change also travels. The merged result runs through `validVolume()` before it is saved. If it fails, nothing changes and the sheet says "These copies couldn't be combined. Nothing was changed."
  - `upd` is set at every write: `submitExpense`, settlement add and edit, member add and rename, and volume edit. `deleteExpense`, `deleteSettle` and `removeMember` push tombstones through `tombstone()`, capped at 20000. `validVolume()` checks `deleted`. `cleanVolume()` keeps `upd` (via `updOf`: an integer from 0 to now+1 day, otherwise 0) and `deleted` (ID_RE, deduped), so both survive `normalize()` on reload.
  - `importSheet()`: when you already have the volume, "Merge with my copy" is the default, the member picker is hidden, and Replace and Keep both remain as options. Before you tap Import it shows "Adds 2 · updates 1 · removes 3", and afterwards the toast reads "Merged: adds 2 · updates 1 · removes 3". `parseCode()` reads from the first `{` to the last `}`, so codes pasted with chat text around them work. Old codes that have no `upd` or `deleted` still import. Deleting a payment now goes through `confirmSheet("Delete payment?")` (critic's not_doing point).
  - R1-2: `memberChip` is now `<button data-act=editMember aria-label="Mira, owes ¥X, edit">`. `editMemberSheet()` handles rename and reuses `nameProblem()`, which `memberSheet` now shares (trims, collapses spaces, rejects case-insensitive duplicates and "you"). Remove is disabled with a visible reason (`#s-why`) when `memberUsed()` is true (paidBy, any splits key including 0 cents, any inputs key, settlement from/to) and for yourself. On merge, a member tombstone is dropped and the member comes back when merged entries still use them. `submitExpense` now stores `inputs` only for members who are in the split.
  - R1-3: Settings > Data has a new row, "Save backup" (`downloadBackup()`, which writes `hati2-backup-YYYY-MM-DD.json` as `{app:'hati2-mock',kind:'backup',v:1,volumes}`) and "Restore" (opens the import sheet, which accepts the file). Backups are capped at 500 volumes and each volume is validated on its own. A volume you don't have keeps its own meId; one you do have is merged. The summary reads "Restores 2 volumes, merges 1 volume you already have." Settings are not included. `save()` calls `navigator.storage.persist()` once. I kept the designer's `sendCode`/`copyShareCode` unchanged; they already meet the AbortError and gesture amendments.
- Tests: the `web/tests/e2e.js` block "R1-1/R1-2/R1-3 (code)" has 35 new checks, all driven through the UI with two to five contexts. It covers: the full two-phone repro; edit vs delete; edit vs edit; payment deletion with confirm; member Z removed on A but used on B; reload, then merging an older copy again without the deleted entry coming back; an invalid merge (more than 200 members) leaving S untouched; a far-future `upd` ignored; an old-format code; chat text around a code; rename to Mia everywhere; a duplicate name blocked; Remove disabled with a reason, including for a 0-cent exact split, which survives a reload; you can't remove yourself; removing an unused member; a draft naming a removed member ignored; the stubbed `navigator.share` text importing on another context; the backup download restoring on a fresh phone and merging on a phone that already has the volume. One existing check changed order: it now picks `#s-mode` before `#s-who`, because the picker only shows for Replace or Keep both. `PORT=8772 web/test.sh` passes 86 of 86 with zero console errors (scratchpad/loop/r1/code/test/e2e.txt). Settings is 1243px at 360 with the backup row, under the 1300 limit.
- Screenshots: /tmp/claude-0/-home-user-HATI-v2/aa252cc6-0787-543f-8ff4-5d833c851890/scratchpad/loop/r1/code/{member-sheet-360,import-merge-360,settings-360}.png
- For @planner: please add to Decisions as a known limitation that last-writer-wins uses each phone's clock (`upd`), so a phone with a badly wrong clock can lose an edit race (never an entry). Volume fields follow the newer volume `upd`.
- For @code-auditor: try to break `mergeVolumes`. Duplicate member names from two phones adding "Sam" separately is allowed, because ids differ. Replace mode drops local tombstones by design.
- For @design-auditor / @designer: please score the new Edit member sheet (disabled Remove plus reason line), the merge summary box in the import sheet (it reuses `.share-sum`), and the Data panel's second button row.
- Disagree: none. I applied every critic amendment for R1-1 to R1-3 and the deleteSettle confirm.
