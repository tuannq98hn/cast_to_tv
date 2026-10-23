#!/usr/bin/env bash
set -Eeuo pipefail

# Architecture verification entrypoint.
# Add project-specific checks here: import boundaries, forbidden dependencies,
# file size limits, naming rules, generated docs freshness, etc.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

failures=0
fail() {
  echo "[architecture][fail] $*" >&2
  failures=$((failures + 1))
}
pass() {
  echo "[architecture][ok] $*"
}

[[ -f AGENTS.md ]] && pass "AGENTS.md exists" || fail "AGENTS.md is missing"
[[ -f ARCHITECTURE.md ]] && pass "ARCHITECTURE.md exists" || fail "ARCHITECTURE.md is missing"
[[ -f docs/validation/architecture-rules.md ]] && pass "architecture rules exist" || fail "docs/validation/architecture-rules.md is missing"

# Prevent committed secrets by filename. In a git repo only tracked/staged files count,
# so a local, git-ignored .env does not fail the gate.
secret_re='(\.pem|\.p12|\.keystore|\.jks|\.mobileprovision|(^|/)\.env(\..+)?|AuthKey_.*\.p8)$'
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  hits="$(git ls-files --cached | grep -E "$secret_re" | grep -v -E '\.env\.example$' || true)"
else
  hits="$(find . -path ./.git -prune -o -path ./node_modules -prune -o -type f -print | sed 's#^\./##' | grep -E "$secret_re" | grep -v -E '\.env\.example$' || true)"
fi
if [[ -n "$hits" ]]; then
  fail "Potential secret/config file tracked: $(echo "$hits" | tr '\n' ' ')"
else
  pass "No secret files tracked"
fi

# Board state must stay valid JSON (agents must use the harness CLI, not hand edits).
for f in .harness/tasks.json .harness/issues.json; do
  if [[ -f "$f" ]]; then
    node -e "JSON.parse(require('fs').readFileSync('$f','utf8'))" && pass "$f is valid JSON" || fail "$f is corrupted"
  fi
done

# ---- CastToTV rules (docs/validation/architecture-rules.md) ----
APP=app/src/main/java/com/casttotv/screen/mirroring

# Play Billing is wrapped by data/Premium.kt only.
hits="$(grep -rlE '^import com\.android\.billingclient' "$APP" | grep -v '/data/Premium.kt$' || true)"
[[ -z "$hits" ]] && pass "Billing only in data/Premium.kt" || fail "Billing imported outside data/Premium.kt: $hits"

# Ads and analytics SDKs were removed in the port; do not bring them back without an ADR.
hits="$(grep -rlE '^import (com\.google\.firebase|com\.google\.android\.gms\.ads|com\.applovin|com\.facebook\.ads|com\.appsflyer|com\.adjust|com\.ikame)' app/src connectsdk/src || true)"
[[ -z "$hits" ]] && pass "No ads/analytics SDKs" || fail "Ads/analytics SDK imported: $hits"

# connectsdk is a library: it must never know about the app.
hits="$(grep -rlE '^import com\.casttotv' connectsdk/src || true)"
[[ -z "$hits" ]] && pass "connectsdk does not import app code" || fail "connectsdk imports app code: $hits"

# Raw TV/network protocol code lives in tv/ and mirror/, not in ui/.
# ponytail: grandfathered list, shrink it as files are fixed (see tech-debt-tracker TD-004).
allowed_ui_net='/ui/activities/function/remote/ChannelFragment.kt$'
hits="$(grep -rlE '^import (okhttp3|org\.java_websocket|io\.socket|dadb|java\.net\.(Socket|ServerSocket))' "$APP/ui" | grep -vE "$allowed_ui_net" || true)"
[[ -z "$hits" ]] && pass "ui/ has no raw network clients" || fail "ui/ imports network clients (move to tv/): $hits"

if [[ "$failures" -ne 0 ]]; then
  echo "[architecture] $failures failure(s)"
  exit 1
fi

echo "[architecture] Verification completed"
