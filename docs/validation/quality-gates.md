# Quality Gates

A change is complete only when the relevant gates pass.

## Default Gate

Run:

```bash
bash scripts/agent/run_quality_gate.sh
```

What it runs, in order:

1. `scripts/agent/check_context.sh` — required agent docs exist
2. `scripts/ci/verify_architecture.sh` — import boundaries, no secrets tracked
3. `./gradlew :app:assembleDebug :app:lintDebug` — both modules compile; no lint errors beyond `app/lint-baseline.xml`
4. `scripts/test/run_all_tests.sh` → `./gradlew testDebugUnitTest` — JVM unit tests of both modules

Takes ~1 minute warm. Not in the gate (needs hardware): `scripts/test/run_ui_tests.sh` (instrumented tests on a
device) and casting to a real TV — record those as manual verification.

Never regenerate the lint baseline to make the gate pass; fix the new issue. Regenerating
(`./gradlew :app:updateLintBaseline`) is a reviewed, explicit task.

## Gate Categories

| Gate | Required When | Examples |
|---|---|---|
| Static analysis | Any code change | `:app:lintDebug`, Kotlin/Java compile |
| Unit tests | Logic changes | `app/src/test` JUnit 4 (protocol encoding, URL building, prefs logic) |
| TV integration | `tv/`, `mirror/`, connectsdk changes | manual on a real TV of each affected brand |
| UI tests | UI or flow changes | device/emulator screenshots via `adb shell am start` (debug exports all Activities) |
| Security review | Billing, pairing, local servers, permissions | `docs/SECURITY.md` checklist |
| Docs update | Behavior, architecture, ports, prefs keys | spec, ADR, ARCHITECTURE port table, db-schema |

## Allowed Exceptions

If a gate cannot run, document:

- Which gate was skipped
- Why it was skipped
- Risk level
- Manual verification performed
- Follow-up task or tech debt ID
