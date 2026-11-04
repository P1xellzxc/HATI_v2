---
name: hati-ship
description: Ship a HATI² round safely: run the tests, rebuild deterministically, make sure installed copies update, commit with the house format, push with retries, and republish the preview. Use for the ship step of each round.
---

# HATI² ship

## Goal first

Use this skill only for work that moves the project goal in `web/README.md`: **G1** works on Android and iOS from a link (offline included), **G2** real group use with correct money and no data loss, **G3** not AI slop. Say which criterion your use of this skill serves. G1: what ships must reach installed phones; G2: nothing broken ships.

## Deeper version of this task
1. **Top professional:** never ships red; checks that the build is reproducible and that existing installs will actually update; writes commit messages that explain why.
2. **Rawest material:** `git status`/`git diff`, the raw `web/test.sh` output, `docs/app/sw.js` cache name, and the remote branch state.
3. **Methods/tools:** `web/test.sh <dir>`; `python3 -I web/tools/build.py` twice → no diff (deterministic); confirm the `CACHE` name in `docs/app/sw.js` changed when shipped files changed (the build hashes content); `git push` with 2/4/8/16 s backoff and a 90 s timeout per attempt.
4. **What shows it's right:** tests green, second build shows no diff, cache name changed iff app files changed, push confirmed by `git status -sb` showing no ahead/behind.

## Checklist
1. `web/test.sh <dir>` — no FAIL lines.
2. Rebuild; `git status` shows no further change.
3. Stage only project files (`web/`, `docs/`, `.claude/`, `.gitignore`); never stage throwaway scripts or screenshots.
4. Commit: first line summarizes user-visible changes; body lists them; end with the two attribution lines the orchestrator gives you.
5. Push with retries; verify.

