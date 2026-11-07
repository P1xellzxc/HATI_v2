---
name: hati-sync
description: Own HATI² live sync without accounts (Cloudflare Worker + one Durable Object per volume, end-to-end encrypted blob, key in the invite link fragment): protocol, server in sync/, client sync in the web app, offline queue, conflict merge and the deploy. Use when building, changing, auditing or deploying anything that moves a volume between phones.
---

# HATI² live sync

## Goal first

Use this skill only for work that moves the project goal in `web/README.md`: **G1** works on Android and iOS from a link (offline included), **G2** real group use with correct money and no data loss, **G3** not AI slop. Say which criterion your use of this skill serves. This is G2 (criterion 6, live sync): two or more people use one volume and see each other's changes live when online, with no login, and nothing is lost offline.

## Decide, don't wait

Decisions belong to the skill whose specialty they are (see "Decision owners" in `brain/Team log.md`). Sync protocol, server, encryption, conflict handling and the sync deploy are yours. Visual design of invites goes to hati-design, words to hati-copy, money math to hati-money-parity.

The owner has delegated decisions inside the project to the team. When a choice comes up in your area, make it yourself against the goal (G1–G3), using the evidence you have; prefer the reversible, lowest-risk option that unblocks the goal; record it under Decisions in `brain/Team log.md` with the reason; and carry on. Ask the owner only for actions outside the repository that only they can take (for example adding the Cloudflare secrets to the repo).

## Before you start: deepen your assignment

Apply this prompt to the specific assignment you were just given (not to the skill in general), every time:

> "I want a deeper version of this task, not a bigger one. Before rewriting it: (1) say what a top professional in this field would do here that I'm not asking for; (2) name the rawest material they would start from instead of what has already been digested for me; (3) name the specific methods and tools they would use; (4) say what, other than you or me, would show the result is right. Then rewrite my prompt to ask for exactly that."

Write your four answers and the rewritten assignment (a few lines each) at the top of your entry in `brain/Team log.md`, then do the rewritten assignment. Deeper means more rigorous within your role and the goal, never more scope.

## Working together

Read `brain/HATI Brain.md`, `brain/Goal.md` and the Decisions in `brain/Team log.md` first. Write findings, decisions and handoffs to `brain/Team log.md` (to @coder for client code, @designer for invite UI, @ios for Safari/home-screen behavior, @android-build for the APK origin). After work run `python3 -I tools/brain/export.py` and commit `brain/` with the change.

## Deeper version of this task
1. **Top professional:** treats sync as a distributed-systems problem, not a feature: defines the protocol and its failure modes first (two phones editing offline, a lost write, a replayed message, a dead socket, a stale tab), and proves convergence instead of demoing the happy path. Treats the server as untrusted: it stores ciphertext only.
2. **Rawest material:** `sync/` (Worker, Durable Object, wrangler.toml, `sync/README.md` protocol), `mergeVolumes`/`validVolume`/`cleanVolume` in `web/src/app.src.html`, the Cloudflare Workers, Durable Objects and WebSocket Hibernation docs, WebCrypto AES-GCM, and real network traces (DevTools/Playwright `page.on('websocket')`).
3. **Methods/tools:** optimistic concurrency (baseVersion → 409 → merge → retry); hibernating WebSockets that broadcast only a version number; AES-GCM-256 with a fresh 96-bit IV per write, key only in `#join=<room>.<key>`; an offline outbox flushed on `online`/reconnect with backoff; vitest (`@cloudflare/vitest-pool-workers` or miniflare) for the server; Playwright with two browser contexts (one iPhone, one Android profile) against `wrangler dev` for end-to-end; `context.setOffline(true)` for offline edits.
4. **What shows it's right:** two contexts edit the same volume (both online, then one offline) and end with identical volumes and balances to the cent; the server's stored bytes contain no member name or amount; a 409 race never loses an expense; the deployed Worker answers from the APK origin and from GitHub Pages; CI deploy log shows the Worker version.

## Rewritten prompt (this is how you operate)

For your assignment, start from the protocol in `sync/README.md` and the merge code in the web app. List the failure modes your change touches, then build or check it with tests that reproduce each one: server tests in `sync/`, and a two-context Playwright scenario (iPhone + Android, one going offline) added to `web/tests/e2e.js` and `web/tests/scenarios.md`. Show that both sides converge to identical volumes, that the server only ever sees ciphertext, and that no write is lost on conflict. Report each failure mode as covered (with the check's name) or open.

## Invariants
- The server never sees plaintext or the key; the key lives only in the URL fragment and on the device.
- Every write carries baseVersion; a mismatch returns 409 and the client merges with `mergeVolumes`, then retries. No last-writer-wins that can drop an expense.
- Merged data still passes `validVolume()` then `cleanVolume()`; money stays integer cents.
- Offline first: the app works fully without the server; pending changes are queued and sent on reconnect.
- Allowed origins: `https://p1xellzxc.github.io` and `https://appassets.androidplatform.net` only. Blob at most 512 KB.
- No accounts, no tracking, no third-party scripts.
- Owner rule: in Cloudflare touch ONLY the `hati-sync` Worker and its own resources. Never change, deploy, delete or rebind anything else in the account (`geryme-fernando-cv`, `geryme-root-redirect`, their D1, R2, secrets, routes or domains). Read-only listing is fine.
