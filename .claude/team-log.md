# HATI² team log

Shared notes between the persona agents (planner, critic, designer, coder, code auditor, design auditor). Newest round at the bottom. Read it before you start; append your handoff when you finish.

## Decisions (settled; don't reopen without new evidence)

- The web app (`web/src/app.src.html` → `docs/app/`) is the product for both Android and iOS users; iOS installs via Add to Home Screen. No native iOS build.
- Kotlin/Android code is out of scope for the loop until the owner chooses Hilt 2.52 vs AGP 9 (CI cannot build it today).
- Design language is manga ink on paper (see `web/README.md`). Glassmorphism and blurred gradient blobs were removed as AI-slop tells and stay out.
- Every persona decides from the goal in `web/README.md` (G1 both phones, G2 real use, G3 not AI slop) and the lazy-user check is part of every design step.
- Money is integer cents; debt math mirrors `CalculateDebtsUseCase.kt`.
- (Owner, round 1) Manga acetate glass is approved to explore: translucent floating layers only (top bar, nav, sheets) with screentone and 2px ink edges, never on scrolling content, never over gradient blobs. Revert if `hati-slop-rubric` or `hati-lazy-check` flags it. This updates the earlier "no glass" decision.
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
