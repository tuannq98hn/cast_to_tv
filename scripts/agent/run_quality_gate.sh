#!/usr/bin/env bash
set -Eeuo pipefail

# CastToTV quality gate. The loop runs this after every submitted task.
# Exit code 0 = pass. Any failing check fails the gate.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

log() { printf '\n[quality-gate] %s\n' "$*"; }
failures=()
check() {
  local name="$1"; shift
  log "$name"
  if "$@"; then
    printf '[quality-gate][ok] %s\n' "$name"
  else
    printf '[quality-gate][FAIL] %s\n' "$name" >&2
    failures+=("$name")
  fi
}

check "agent context" bash scripts/agent/check_context.sh
check "architecture rules" bash scripts/ci/verify_architecture.sh
# Compiles both modules; lint fails only on issues not in app/lint-baseline.xml.
check "build + lint" ./gradlew --console=plain :app:assembleDebug :app:lintDebug
check "tests" bash scripts/test/run_all_tests.sh

if [[ ${#failures[@]} -gt 0 ]]; then
  printf '\n[quality-gate] FAILED: %s\n' "${failures[*]}" >&2
  exit 1
fi
log "PASSED"
