# QUALITY_SCORE.md

This document tracks repo quality over time.

## Current Score

| Area | Score 1-5 | Notes |
|---|---:|---|
| Architecture | 3 | Clear package split (ui/tv/mirror/data); global mutable singletons (`TvConnect`), one UI file with raw HTTP |
| Tests | 1 | No unit or instrumented tests yet; JUnit 4 is wired, gate runs `testDebugUnitTest` |
| Reliability | 2 | Many failure paths on TV protocols untested; behaviour only verified manually on devices |
| Security | 2 | Unauthenticated LAN servers, global cleartext, SSL proceed option (all inherited, see SECURITY.md) |
| Documentation | 3 | Architecture, ports, prefs schema, specs index documented 2026-10-23 |
| Developer Experience | 3 | One-command build (~20 s), gate with lint baseline; no CI yet |
| Agent Readiness | 4 | Gate + architecture checks wired; 11 skills in `.agents/skills/`; no project-specific skills yet |

## Scoring Guide

- 1 = risky, unclear, or mostly missing
- 2 = partially present but inconsistent
- 3 = usable baseline
- 4 = strong and mostly automated
- 5 = excellent, automated, and maintained

## Quality Review Notes

- 2026-10-23 — Harness adapted to the Android project: gate = context + architecture greps + assembleDebug +
  lintDebug (baseline: 406 errors / 1417 warnings grandfathered) + JVM unit tests.
