---
title: "Web app"
tags: ["hati/tech"]
---

# HATI² web app

The installable app for Android and iOS testers (Add to Home Screen). One source file, one build, one test command.

| What | Where |
|---|---|
| Source (HTML, CSS, JS in one file) | `web/src/app.src.html` |
| Palettes (generated from Android) | `app/src/main/java/com/hativ2/ui/theme/HatiPalette.kt` → `web/tools/export_palettes.py` |
| Build | `python3 -I web/tools/build.py [--artifact PATH]` → `docs/app/` |
| Test (build + serve + 36+ e2e checks) | `web/test.sh [screenshot-dir]` |
| Fonts (self-hosted) | `web/fonts/` |
| Screenshot kit (+ contrast) | `web/shots.sh <dir> [--contrast]` → contact sheet `<dir>/index.html` |
| Real-use scenarios | `web/tests/scenarios.md` |
| Pixel inspector | `URL=... node web/tests/pixel.js [--width 360] [--mode dark]` |

Never edit `docs/app/` by hand; it is build output.


iOS rules: [[hati-ios]] · Shipping: [[hati-ship]] · Back to [[HATI Brain]]
