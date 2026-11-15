---
title: "hati-test-scout"
tags: ["hati/skill"]
---

> Find the HATI² tests nobody has written yet and keep one place that runs every test. Inventories all suites (web e2e, contrast, data-io, receipt, sync server, Android lint/unit/emulator in CI), researches the risks no test covers (two phones, identity, links between apps, storage, performance), writes the missing checks and runs everything with tools/test-all.sh. Use before shipping, after a real-use bug report, or when asked to "run all tests, including the ones we don't have".

Source: `.claude/skills/hati-test-scout/SKILL.md` · Back to [[HATI Brain]] · [[Skills index]]


# HATI² test scout

## Goal first
Serves **G2** (real use, no wrong money or data) and criterion 1 (`web/test.sh` passes with a check for every shipped behavior). A bug the owner found on a real phone means a test was missing: find the class of tests it belongs to, not just the one case.

## What exists (keep this list current)
| Suite | Where | Runs |
|---|---|---|
| e2e (500+ checks, Playwright, mock sync server) | `web/tests/e2e.js` via `web/test.sh` | here |
| Text contrast, 13 palettes × 2 themes | `web/tests/contrast.js` | here |
| Backup / share code / invite bytes on desktop, Android app, iPhone | `web/tests/data-io.js` | here |
| Receipt parser table | `web/tests/receipt.js` | here |
| Sync server (Worker + Durable Object) | `sync/test/` (vitest), `npx tsc --noEmit` | here (`npm ci` first) |
| Native app lint + unit tests | `app/src/test/` | GitHub CI |
| Web-app APK shell on an emulator (Back, links, storage, bridge) | `webapk/src/androidTest/.../WebShellTest.kt` | GitHub CI (no KVM here) |
| Pixel and screenshot inspectors | `web/tests/pixel.js`, `web/shots.sh` | here, on demand |
| Real-use scenarios (whole jobs, taps, money to the cent) | `web/tests/scenarios.md` | each has a named e2e check |

Run everything here with `tools/test-all.sh`; read the Android results on the PR's checks.

## How to find the tests we don't have
1. Start from the report, in the owner's words, and replay it with real data (the owner's backup in the conversation, two or three browser contexts on the shared mock server: one per phone, one per app/browser storage).
2. Ask, for every flow touched: which **state already on the phone** changes the outcome (an older copy, someone else's backup, a deleted twin, an archived volume)? Which **surface** runs it (Chrome, the APK WebView, iPhone Safari, the home-screen app), and do they share storage? (They don't.) Which **order** of events (offline then online, two edits at once, app killed mid-step)?
3. For each answer, write a check that fails on today's code first. Prefer properties (300 random cases) over one example where the rule is general (money sums, who "you" are, merges).
4. Route the fix to the skill that owns it (`brain/Team log.md`, Decision owners). Personas for the classes we kept missing: **[[hati-identity]]** (who "you" are across phones), **[[hati-android-device]]** (the APK shell, links between apps, Back), **[[hati-perf]]** (time budgets), **[[hati-sync]]**, **[[hati-money-parity]]**.
5. Write what you added to `brain/Team log.md` and to the table above.

## Lessons (real bugs that had no test)
- 2026-11-14: an invite opened in Chrome joined there; the app (separate storage) stayed empty. → HO checks.
- 2026-11-14: Back closed the APK from inside a volume (a replaced history entry). → AB checks + emulator test.
- 2026-11-15: Ivy's app showed Geryme's side after joining: her app already had copies with Geryme as "you", and a merge kept it. → ME checks, property "merges never change who you are".
