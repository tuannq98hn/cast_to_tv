---
name: systematic-debugging
description: Find the root cause with evidence before changing code. Use for any bug, failing test, crash, or unexpected behavior.
---
# Systematic debugging

1. **Reproduce.** Get a failing test or exact steps. No repro → no fix; write down what you tried.
   Start from `docs/bugs/<I-ID>.md` if it exists.
2. **Read the evidence.** Full stack trace, `adb logcat -d | grep -A30 'FATAL EXCEPTION'`, Gradle output,
   test report `app/build/reports/tests/testDebugUnitTest/index.html`.
3. **Trace the flow** end to end (UI → ViewModel → repository → DAO). Find every caller of the function
   you suspect (`grep -rn "fnName" app/src`).
4. **One hypothesis at a time.** Predict what you'd see if it's true, check it (log, test, debugger). Wrong → next.
5. **Fix the root cause** in the shared place all callers route through — not a guard in the one caller the
   ticket names.
6. **Prove it.** The repro test now passes (remove `@Ignore`), the gate passes.

Stop and `task block` after 3 failed hypotheses — write what you ruled out.
Android traps to check early: lifecycle (rotation/process death), main-thread DB access, Flow not collected,
stale `build/` (`./gradlew clean`), wrong `compileSdk`/dependency versions.
