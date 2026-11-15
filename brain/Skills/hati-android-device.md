---
title: "hati-android-device"
tags: ["hati/skill"]
---

> Own how the HATI² APK behaves as a real Android app: the WebView shell (webapk/), links that should open the app (https invites, hati2://, Chrome's intent hand-off, verified app links), Back, storage separate from Chrome, file save/share bridge, renderer restarts. Use when something works in the browser but not in the installed app, or when changing MainActivity, the manifest or how invites reach the app.

Source: `.claude/skills/hati-android-device/SKILL.md` · Back to [[HATI Brain]] · [[Skills index]]


# HATI² Android app (device behavior)

## Goal first
Serves **G1** (installs from a link and works as an app) and **G2** (data ends up in the app the person uses).

## Facts that caused real bugs
- The APK's storage (origin `appassets.androidplatform.net`) is **not** Chrome's. Anything done in Chrome stays in Chrome.
- Android 12+ opens an https link in an app only for a **verified** domain (`/.well-known/assetlinks.json` at the domain root). `p1xellzxc.github.io` has no user site yet, so invites open in Chrome, which hands them to the app: `intent://join?j=<payload>#Intent;scheme=hati2;package=com.hativ2.web;S.browser_fallback_url=…?noapp=1#…;end` (`appHandoff()`); the app maps `hati2://join?j=` in `MainActivity.startUrl`. Ready for the owner: `webapk/assetlinks.json` (HATI2 key SHA-256) + `autoVerify` in the manifest.
- Back asks the page first (`window.hatiBack()`), never trusts WebView history alone.

## How to test
- Here (no SDK/KVM): e2e with `HatiAndroid` stubbed for "inside the app", Pixel 7 without it for Chrome (`handoff:true` to see the hand-off; default contexts carry `?noapp=1` = no app installed). Blocks AB, HO, ME.
- In CI on an emulator: `webapk/src/androidTest/java/com/hativ2/web/WebShellTest.kt` (bundled app renders, invite fragment, Back inside a volume, hand-off link reaches the join sheet, start URL mapping). Add a test there for every shell change.
- On a real phone (owner): install the APK from the release, tap an invite in Messenger, report what opens.

## Decide, don't wait
Shell and manifest are yours with [[hati-android-build]] (Gradle, versions, signing). Record in `brain/Team log.md`.
