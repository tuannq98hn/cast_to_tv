---
name: reviewing-code
description: Review a submitted change against acceptance criteria and project rules, in a fixed order, with actionable feedback.
---
# Reviewing code

Check in this order; stop at the first blocker but list everything found so far:

1. **Scope** — the diff does what the task asks, nothing unrelated.
2. **Correctness** — acceptance criteria met; edge cases (empty, error, rotation/process death, long text).
3. **Data safety** — no path loses entries; Room schema change has a `Migration` + test; no
   `fallbackToDestructiveMigration()`.
4. **Privacy** — no entry content in logs/analytics; no new network/SDK without ADR (`docs/SECURITY.md`).
5. **Architecture** — layer rules in `docs/validation/architecture-rules.md`; DB/IO off the main thread.
6. **Tests** — new logic has unit tests that would fail without the change; bug fix un-skipped its repro test.
7. **Android hygiene** — strings in resources, no hardcoded colors/dimens, components not exported.
8. **Docs** — spec/plan/ADR/test-matrix updated when behavior or structure changed.

Feedback format: `file:line — problem — what to do`. Separate **blocking** from **nit**.
Re-run the evidence yourself (gate, the named test) rather than trusting the summary.
