#!/usr/bin/env bash
set -Eeuo pipefail

# Bootstrap local development for CastToTV (Android, Gradle wrapper).
# Needs: JDK 17+ on PATH, Android SDK (platform 35) via local.properties or ANDROID_HOME, Node.js 18+ for the harness.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

log() { printf '\n[bootstrap] %s\n' "$*"; }
die() { printf '\n[bootstrap][error] %s\n' "$*" >&2; exit 1; }

command -v java >/dev/null 2>&1 || die "java not found (install JDK 17+)"
command -v node >/dev/null 2>&1 || die "node not found (Node.js 18+ runs the agent harness)"

if [[ ! -f local.properties ]]; then
  sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
  [[ -d "$sdk" ]] || die "Android SDK not found; set ANDROID_HOME or create local.properties with sdk.dir=..."
  echo "sdk.dir=$sdk" > local.properties
  log "Wrote local.properties (sdk.dir=$sdk)"
fi

log "Resolving dependencies and compiling debug"
./gradlew --console=plain :app:assembleDebug

[[ -d .harness ]] || node harness/cli.mjs init
log "Bootstrap completed — APK: app/build/outputs/apk/debug/app-debug.apk"
