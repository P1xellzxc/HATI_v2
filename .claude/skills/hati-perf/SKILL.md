---
name: hati-perf
description: Own HATI² speed budgets and their tests: the M-perf checks (4x CPU throttle, long tasks during screen animations, input task, frame pacing), render cost of long lists (History 100 rows), and finding the root cause of a slow or flaky timing check instead of calling it a flake. Use when M-perf fails, a screen feels slow on a cheap phone, or a change adds work to rendering.
---

# HATI² performance

## Goal first
Serves **G1** (usable on a cheap Android phone) and criterion 1 (`web/test.sh` green, which needs M-perf to pass reliably).

## Budgets (web/tests/e2e.js, block M)
At 4x CPU throttle, 3 runs each, medians: no long task over 50 ms after the animation's first frame; the input task under 50 ms or within 15 ms of the reduced-motion run; frame median ≤ 17.5 ms; at most 1 slow frame.

## History (fixed 2026-11-15, measured)
"volume to history (100 rows)" sat at the 50 ms line on old main too (1 of 3 isolated runs failed). Profiled at 4x: the page layout forced by `focusTitle()` (~24 ms) plus building the HTML (~13 ms) for 100 rows. Fix: `histBody()` draws the first `HIST_FIRST` = 40 rows with the screen and `histMore()` adds the rest in a separate task just after the screen change (unfiltered, not selecting). Result: 5 of 5 isolated runs pass, and the full suite is green. Check "PERF: …" proves the split ends identical to one draw.

## How to work
Never re-run until green. Run the M block alone (header of e2e.js + block M) several times on the base and on the change, compare medians, profile the slow task, fix its cause, show the before/after numbers in `brain/Team log.md`.
