---
name: verification-before-completion
description: Run the real checks and quote their output before claiming anything is done, fixed, or passing.
---
# Verification before completion

Never write "done", "fixed" or "tests pass" without fresh evidence from this turn.

1. Run the gate: `bash scripts/agent/run_quality_gate.sh` → must end with `[quality-gate] PASSED`.
2. Run the specific test for your change and note its name.
3. UI change → also follow `ui-evidence` (screenshot/logcat), instrumented tests if a device is connected.
4. Check every acceptance criterion of the task one by one; say how each is met.
5. `git status` / `git diff --stat` (if git exists): only files the task needs changed.

In the submit summary quote: gate result line, tests run, criteria → evidence, anything skipped and why.
If a check could not run (no device, no network), say so — do not round it up to "passed".
