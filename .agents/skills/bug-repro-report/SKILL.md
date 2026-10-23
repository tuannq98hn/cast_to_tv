---
name: bug-repro-report
description: Write docs/bugs/<ISSUE>.md so anyone can reproduce the bug, with environment, steps, evidence, and a skipped failing test.
---
# Bug repro report

1. `cp docs/bugs/_template.md docs/bugs/<I-ID>.md`; fill it as you go.
2. Environment: emulator/device + API level (`adb shell getprop ro.build.version.sdk`), app `versionName`,
   debug/release, fake test data used.
3. Steps: numbered, exact taps or commands. Smallest set that triggers it.
4. Expected vs actual + evidence: trimmed stack trace / `adb logcat -d` lines, screenshot path (`ui-evidence`).
5. Frequency (always / N of M) and what does **not** trigger it.
6. Suspected area `file:line`, marked as a guess.
7. Failing test that captures it, annotated `@Ignore("I-XXX: repro, remove when fixed")` (JUnit4), in
   `app/src/test` if possible. Put its path in the report.
8. Register: `node harness/cli.mjs issue repro <I-ID> --file docs/bugs/<I-ID>.md --test <path>`;
   add a row to `docs/bugs/README.md`.

Never put real journal content in the report — use fake text.
