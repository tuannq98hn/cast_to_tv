#!/usr/bin/env bash
set -Eeuo pipefail

# JVM unit tests (JUnit 4) for both modules: app/src/test, connectsdk/src/test.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT_DIR"

./gradlew --console=plain testDebugUnitTest "$@"
