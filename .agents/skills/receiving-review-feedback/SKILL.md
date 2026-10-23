---
name: receiving-review-feedback
description: Handle reviewer feedback on a retried task — verify each point, fix it, or push back with evidence.
---
# Receiving review feedback

1. `node harness/cli.mjs task show <T-ID>` — read every feedback item from the last attempt first.
2. For each item: reproduce/confirm it in the code. Correct → fix it. Wrong → don't change code; explain why
   with evidence (file:line, test output).
3. Fix blocking items before nits. Don't start unrelated refactors.
4. Re-run the gate and the affected tests.
5. In the submit summary, answer every item: `fixed (how)` or `not changed (why + evidence)`.
