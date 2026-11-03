# HATI² web app

The installable app for Android and iOS testers (Add to Home Screen). One source file, one build, one test command.

| What | Where |
|---|---|
| Source (HTML, CSS, JS in one file) | `web/src/app.src.html` |
| Palettes (generated from Android) | `app/src/main/java/com/hativ2/ui/theme/HatiPalette.kt` → `web/tools/export_palettes.py` |
| Build | `python3 -I web/tools/build.py [--artifact PATH]` → `docs/app/` |
| Test (build + serve + 36+ e2e checks) | `web/test.sh [screenshot-dir]` |
| Fonts (self-hosted) | `web/fonts/` |

Never edit `docs/app/` by hand; it is build output.

## Invariants

- Money is integer cents. Splits always sum exactly to the expense amount; balances across members always sum to zero.
- Every interpolated value in a template goes through `esc()`. Imported share codes pass `validVolume()` then `cleanVolume()`.
- Debt math mirrors `domain/usecase/CalculateDebtsUseCase.kt` (fewest payments and pairwise).
- Data lives in `localStorage` key `hati2.mock.v1`; a share code is the only way to move a volume between phones.
- Design language: manga ink on paper (see `DESIGN.md` and `ui/components/MangaComponents.kt`): flat paper, 2px ink borders, 3px corners, hard offset shadow only on pressable things, ruled ledgers instead of one card per row, Wada palette accent used sparingly.

## Goal

An app Android and iOS users can install from a link and use for real group expenses, that does not read as AI-generated design. Done means:
1. `web/test.sh` passes, including a check for every shipped behavior.
2. Code audit: zero blocker and zero annoying findings that reproduce.
3. Design audit: every AI-slop tell in the rubric scores 0, both themes, at 360 and 390 px.
