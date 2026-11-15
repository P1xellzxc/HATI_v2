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
"volume to history (100 rows)" sat at the 50 ms line on old main too (1 of 3 isolated runs failed). Profiled at 4x: the page layout forced by `focusTitle()` (~24 ms) plus building the HTML (~13 ms) for 100 rows. Fix: `histBody()` draws the first `HIST_FIRST` = 40 rows with the screen; `histMore()` adds the rest `HIST_STEP` = 20 at a time when the end of the list comes within a screen of view (`#histMoreAt` + IntersectionObserver), never during the screen change (unfiltered, not selecting). Checks "PERF: …" prove 40 at first and the same rows, order, months and classes after scrolling.

## Open: the timing check itself is noisy on this machine (2026-11-15)
On the 4-CPU cloud VM, M-perf fails at random on screens no change touched (hub→volume, Back) in the same runs, with long tasks of 51–81 ms from rendering at 4x. Next: make the check robust, not looser: add a control (the same measurement on an unchanged screen in the same run) and judge the change against it, or use the median of 5 runs. Then require 10 green full runs.

## How to work
Never re-run until green. Run the M block alone (header of e2e.js + block M) several times on the base and on the change, compare medians, profile the slow task, fix its cause, show the before/after numbers in `brain/Team log.md`.
