#!/usr/bin/env bash
# Every HATI² test that runs on this machine, in one go (hati-test-scout keeps the list current).
# Usage: tools/test-all.sh [out-dir]. Android lint/unit/emulator tests run in GitHub CI (.github/workflows/ci.yml),
# because this container has no Android SDK or KVM; read their results on the PR's checks.
set -uo pipefail
cd "$(dirname "$0")/.."
OUT="${1:-$(mktemp -d)}"; mkdir -p "$OUT"; fail=0
step() { echo "== $1"; }
step "web: build + e2e + contrast + data-io + receipt (web/test.sh)"
web/test.sh "$OUT/web" > "$OUT/web.log" 2>&1 || fail=1
echo "   $(grep -c '^PASS' "$OUT/web.log") PASS, $(grep -c '^FAIL' "$OUT/web.log") FAIL (log: $OUT/web.log)"; grep '^FAIL' "$OUT/web.log" | cut -c1-200
step "sync server: typecheck + vitest (sync/)"
( cd sync && { [ -d node_modules ] || npm ci --no-audit --no-fund >/dev/null 2>&1; } && npx tsc --noEmit && npx vitest run ) > "$OUT/sync.log" 2>&1 || fail=1
grep -E "Tests|error" "$OUT/sync.log" | tail -3
step "brain export (the vault matches the repo)"
python3 -I tools/brain/export.py > "$OUT/brain.log" 2>&1 || fail=1; tail -1 "$OUT/brain.log"
step "Android (CI only): lint + unit tests, emulator tests (webapk WebShellTest), APK build"
echo "   see the PR's checks; nothing to run here"
[ $fail = 0 ] && echo "ALL PASS" || echo "SOMETHING FAILED (see logs in $OUT)"
exit $fail
