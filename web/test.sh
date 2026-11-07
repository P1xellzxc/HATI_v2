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
cp web/tests/e2e.js docs/app/.e2e.js
cp web/tests/contrast.js docs/app/.contrast.js
trap 'kill $SERVER 2>/dev/null || true; rm -f docs/app/.e2e.js docs/app/.contrast.js' EXIT
(cd docs/app && URL="http://127.0.0.1:$PORT/" node .e2e.js "$SHOTS") | tee "$SHOTS/e2e.txt"
# Text contrast in all palettes and both themes (WCAG 4.5:1); the table goes to contrast.txt.
if (cd docs/app && URL="http://127.0.0.1:$PORT/" node .contrast.js) > "$SHOTS/contrast.txt" 2>&1; then echo "PASS text contrast >= 4.5:1 in every palette and theme"; else echo "FAIL text contrast (see $SHOTS/contrast.txt)"; tail -5 "$SHOTS/contrast.txt"; exit 1; fi
! grep -q '^FAIL' "$SHOTS/e2e.txt"
