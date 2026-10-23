#!/usr/bin/env bash
set -Eeuo pipefail

# Project-level test entrypoint used by the quality gate. Device-dependent tests are not part of it;
# run scripts/test/run_ui_tests.sh separately when a device or emulator is attached.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

bash scripts/test/run_unit_tests.sh
