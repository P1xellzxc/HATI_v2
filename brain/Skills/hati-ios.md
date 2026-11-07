---
title: "hati-ios"
tags: ["hati/skill"]
---

> Check and fix how the HATI² web app behaves as an iPhone home-screen app (WebKit, Add to Home Screen, storage, safe areas, input zoom, offline). Use when planning, coding, or auditing anything that affects iOS users.

Source: `.claude/skills/hati-ios/SKILL.md` · Back to [[HATI Brain]] · [[Skills index]]


# HATI² on iOS

## Goal first

Use this skill only for work that moves the project goal in `web/README.md`: **G1** works on Android and iOS from a link (offline included), **G2** real group use with correct money and no data loss, **G3** not AI slop. Say which criterion your use of this skill serves. This is the iOS half of G1.

## Decide, don't wait

Decisions belong to the skill whose specialty they are (see "Decision owners" in `brain/Team log.md`). If it's yours, decide. If it's another skill's, route it there in the log.

The owner has delegated decisions inside the project to the team. When a choice comes up in your area, make it yourself against the goal (G1–G3), using the evidence you have; prefer the reversible, lowest-risk option that unblocks the goal; record it under Decisions in `brain/Team log.md` with the reason; and carry on. Ask the owner only for actions outside the repository that only they can take (for example enabling GitHub Pages, sharing a private link, accounts or payments).

## Before you start: deepen your assignment

Apply this prompt to the specific assignment you were just given (not to the skill in general), every time:

> "I want a deeper version of this task, not a bigger one. Before rewriting it: (1) say what a top professional in this field would do here that I'm not asking for; (2) name the rawest material they would start from instead of what has already been digested for me; (3) name the specific methods and tools they would use; (4) say what, other than you or me, would show the result is right. Then rewrite my prompt to ask for exactly that."

Write your four answers and the rewritten assignment (a few lines each) at the top of your entry in `brain/Team log.md`, then do the rewritten assignment. Deeper means more rigorous within your role and the goal, never more scope.

## Deeper version of this task
1. **Top professional:** treats iOS Safari as a separate platform with its own rules, not as "a phone browser": tests the installed home-screen app, not just a Safari tab, and writes the in-app guidance iOS users need because iOS never prompts to install.
2. **Rawest material:** the shipped `docs/app/index.html`, `manifest.webmanifest`, `sw.js`; WebKit's documented behavior for home-screen web apps; and, when available, a real iPhone.
3. **Methods/tools:** a checklist review (below); Playwright Chromium with an iPhone profile for layout (WebKit is not installed here and must not be installed); and a manual test script for a real iPhone (below) handed to the user.
4. **What shows it's right:** the checklist passes with code references; the manual script passes on a real iPhone (reported by the user); `web/test.sh` stays green.


## Rewritten prompt (this is how you operate)

For your assignment, walk the checklist below against the shipped `docs/app` files and the source, citing the line that satisfies or breaks each item. Check layout with Chromium and `devices['iPhone 13']`. Report each gap with its severity by goal impact and a concrete fix; when you are the one fixing, add an e2e check where the behavior is testable in Chromium. End with the manual iPhone script below for the user.

## Checklist
- **Install guidance:** iOS has no install prompt. The app must tell iPhone users how to add it (Share → Add to Home Screen), shown only on iOS Safari and not inside the installed app (`navigator.standalone`).
- **Storage:** the home-screen app's storage is separate from Safari's. Say so before users enter data, and make Share codes the backup path.
- **Eviction:** script-written storage can be cleared after weeks of no use in Safari tabs; installed apps are safer. Encourage installing and exporting.
- **Layout:** `viewport-fit=cover`, `env(safe-area-inset-*)` on fixed bars, `100dvh` instead of `100vh`, sticky top bar under the status bar with `black-translucent`.
- **Inputs:** font-size ≥ 16px (no zoom on focus); `inputmode=decimal` for money; no hover-only affordances.
- **Offline:** service worker scope `./`, navigation fallback works in standalone mode; cache name changes with content (build does this).
- **Clipboard/share:** `navigator.clipboard.writeText` only inside a tap handler, with a select-text fallback; `navigator.share` is available on iOS and is a better share path than copy.
- **Icons/meta:** `apple-touch-icon` 180×180, `apple-mobile-web-app-title`, status bar style.

## Manual test script for a real iPhone (give to the user)
1. Open the app link in Safari → see the install hint → Share → Add to Home Screen.
2. Open from the icon; add a volume and an expense; close and reopen: data is there.
3. Airplane mode; reopen: the app opens and shows data.
4. Share a volume; paste the code on a second phone (Android) and import it.
