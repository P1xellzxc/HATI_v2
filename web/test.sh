#!/usr/bin/env bash
# Build the web app, serve docs/app on localhost, and run the end-to-end suite.
# Usage: web/test.sh [screenshot-dir]
set -euo pipefail
cd "$(dirname "$0")/.."
PORT="${PORT:-8765}"
SHOTS="${1:-$(mktemp -d)}"
mkdir -p "$SHOTS"
python3 -I web/tools/build.py
(fuser -k "$PORT/tcp" >/dev/null 2>&1 || true)
python3 -m http.server "$PORT" --bind 127.0.0.1 --directory docs/app >/dev/null 2>&1 &
SERVER=$!
trap 'kill $SERVER 2>/dev/null || true' EXIT
sleep 1
# Playwright resolves from the global install; run from docs/app so relative requires work.
# Per-run names so parallel runs don't delete each other's copies.
E2E=".e2e.$$.js"; CON=".contrast.$$.js"; DIO=".data-io.$$.js"
cp web/tests/e2e.js "docs/app/$E2E"
cp web/tests/contrast.js "docs/app/$CON"
cp web/tests/data-io.js "docs/app/$DIO"
trap 'kill $SERVER 2>/dev/null || true; rm -f "docs/app/$E2E" "docs/app/$CON" "docs/app/$DIO"' EXIT
(cd docs/app && URL="http://127.0.0.1:$PORT/" node "$E2E" "$SHOTS") | tee "$SHOTS/e2e.txt"
# Text contrast in all palettes and both themes (WCAG 4.5:1); the table goes to contrast.txt.
if (cd docs/app && URL="http://127.0.0.1:$PORT/" node "$CON") > "$SHOTS/contrast.txt" 2>&1; then echo "PASS text contrast >= 4.5:1 in every palette and theme"; else echo "FAIL text contrast (see $SHOTS/contrast.txt)"; tail -5 "$SHOTS/contrast.txt"; exit 1; fi
# Data in and out (backup/restore, share code, invite links) on desktop, the Android app and iPhone.
(cd docs/app && URL="http://127.0.0.1:$PORT/" node "$DIO") | tee "$SHOTS/data-io.txt"
# The paste-a-receipt parser table (D9), plain node.
node web/tests/receipt.js | tee "$SHOTS/receipt.txt"
! grep -q '^FAIL' "$SHOTS/e2e.txt" "$SHOTS/data-io.txt" "$SHOTS/receipt.txt"
