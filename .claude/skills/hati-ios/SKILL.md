---
name: hati-ios
description: Check and fix how the HATI² web app behaves as an iPhone home-screen app (WebKit, Add to Home Screen, storage, safe areas, input zoom, offline). Use when planning, coding, or auditing anything that affects iOS users.
---

# HATI² on iOS

## Goal first

Use this skill only for work that moves the project goal in `web/README.md`: **G1** works on Android and iOS from a link (offline included), **G2** real group use with correct money and no data loss, **G3** not AI slop. Say which criterion your use of this skill serves. This is the iOS half of G1.

## Deeper version of this task
1. **Top professional:** treats iOS Safari as a separate platform with its own rules, not as "a phone browser": tests the installed home-screen app, not just a Safari tab, and writes the in-app guidance iOS users need because iOS never prompts to install.
2. **Rawest material:** the shipped `docs/app/index.html`, `manifest.webmanifest`, `sw.js`; WebKit's documented behavior for home-screen web apps; and, when available, a real iPhone.
3. **Methods/tools:** a checklist review (below); Playwright Chromium with an iPhone profile for layout (WebKit is not installed here and must not be installed); and a manual test script for a real iPhone (below) handed to the user.
4. **What shows it's right:** the checklist passes with code references; the manual script passes on a real iPhone (reported by the user); `web/test.sh` stays green.

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

