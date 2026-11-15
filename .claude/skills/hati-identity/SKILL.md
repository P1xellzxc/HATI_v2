---
name: hati-identity
description: Own who "you" are in HATI² on every phone: meId per volume (never synced), the member a join picks, invites that name their person, merges that keep or switch it, "This is me", Settings > Who you are, and links of the same person across volumes. Use when a phone shows a volume from the wrong person's side, or when changing joins, invites, merges, backups or cross-volume links.
---

# HATI² identity ("who you are")

## Goal first
Serves **G2**: every person sees balances from their own side ("You owe Geryme ₱3,135.00", not "Ivy owes you"). A wrong "you" makes every number on the screen read backwards.

## Rules (keep them true, each has a check)
- `meId` is this phone's choice per volume. It is never in the synced blob (`payload()` drops it) and a merge or sync never changes it (property check "ME: in 300 random merges…").
- A copy can arrive with someone else as "you": their backup, their share code, a test on the wrong phone. So every place that brings a volume in must say who you are, and let you change it:
  - new volume from an invite: the picker (single invite) or the invite's person (one-link invite);
  - a volume already here: the single invite says "You're X in it on this phone"; the one-link invite asks "Who are you?" when the phone and the invite disagree, starting on the invite's person, with "I'm <current>" to keep (the sender testing his own link);
  - any time: a member chip → **This is me** (Undo), and **Settings → Who you are → Change**.
- Linked people across volumes (`hati2.links.v1`) never include "you"; switching "you" drops that member from cross-volume rows automatically (`xMember`).

## How to test
Two or three contexts on the e2e mock server: the sender's app (`HatiAndroid` stub), the receiver's app, the receiver's Chrome. Seed the receiver with the sender's copies (as after a backup restore) — that is the case that broke. Check balances from both sides after join, reload and a live change. Block "ME" in `web/tests/e2e.js`.

## Decide, don't wait
Identity behavior is yours; copy goes through hati-copy, money signs through hati-money-parity. Record decisions in `brain/Team log.md`.
