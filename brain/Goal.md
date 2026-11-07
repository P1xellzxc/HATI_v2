---
title: "Goal and definition of done"
tags: ["hati/goal"]
---

## Goal

Every persona skill decides what to do from this goal, not from the task list, and makes decisions in its area itself instead of waiting on the owner (see "Decide, don't wait" in the skills).

An app Android and iOS users can install from a link and use for real group expenses, that does not read as AI-generated design. Done means:
1. `web/test.sh` passes, including a check for every shipped behavior.
2. Code audit: zero blocker and zero annoying findings that reproduce.
3. Design audit: every AI-slop tell in the rubric scores 0, both themes, at 360 and 390 px.
4. Lazy-user check passes on every screen (see the [[hati-design-audit]] skill): understood in 3 seconds, goal in one tap, nothing unfinished.
5. Invites: a volume is shared by a link or the same link as a QR code; the other person (Android or iPhone, no login) opens it and joins the volume, picking who they are. Verified by a two-phone e2e scenario.
6. Live sync (owner goal): two or more people use one volume with no account; when online, a change on one phone shows on the others within seconds; offline changes are queued and merged on reconnect without losing any expense. The server (Cloudflare Worker, `sync/`) stores only end-to-end-encrypted data; the key travels in the invite link. Verified by a two-context e2e scenario (one going offline) and server tests. Owner skill: [[hati-sync]].


## Invariants

- Money is integer cents. Splits always sum exactly to the expense amount; balances across members always sum to zero.
- Every interpolated value in a template goes through `esc()`. Imported share codes pass `validVolume()` then `cleanVolume()`.
- Debt math mirrors `domain/usecase/CalculateDebtsUseCase.kt` (fewest payments and pairwise).
- Data lives in `localStorage` key `hati2.mock.v1`; volumes move between phones by invite link/QR and live sync (see [[hati-sync]]); the share code stays as the offline fallback.
- Design language: manga ink on paper (see `DESIGN.md` and `ui/components/MangaComponents.kt`): flat paper, 2px ink borders, 3px corners (8px `--clay-r` on manga clay controls only), hard offset shadow only on pressable things, which are manga clay (flat palette fill with inset light and shade, no gradients), ruled ledgers instead of one card per row, Wada palette accent used sparingly.


See also [[Decisions]], [[Team loop]], [[HATI Brain]]
