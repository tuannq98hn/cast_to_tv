#!/usr/bin/env bash
set -Eeuo pipefail

# Instrumented tests (app/src/androidTest) on an attached device/emulator.
# There are none yet; for manual UI checks see the ui-evidence skill and docs/validation/test-matrix.md.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

if ! adb get-state >/dev/null 2>&1; then
  echo "[ui-tests] No device/emulator attached (adb devices). Start one and rerun." >&2
  exit 1
fi
./gradlew --console=plain connectedDebugAndroidTest "$@"
