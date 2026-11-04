#!/usr/bin/env bash
# Build, serve, and run the shared screenshot kit (and contrast check with --contrast).
# Usage: web/shots.sh <out-dir> [--contrast]
set -euo pipefail
cd "$(dirname "$0")/.."
PORT="${PORT:-8766}"; OUT="${1:?out dir}"
python3 -I web/tools/build.py
(fuser -k "$PORT/tcp" >/dev/null 2>&1 || true)
python3 -m http.server "$PORT" --bind 127.0.0.1 --directory docs/app >/dev/null 2>&1 &
SERVER=$!; trap 'kill $SERVER 2>/dev/null || true' EXIT; sleep 1
URL="http://127.0.0.1:$PORT/" node web/tests/shots.js "$OUT"
if [ "${2:-}" = "--contrast" ]; then URL="http://127.0.0.1:$PORT/" node web/tests/contrast.js | tee "$OUT/contrast.txt"; fi
