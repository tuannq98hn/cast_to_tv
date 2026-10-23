# ADR-0001: Kotlin rebuild of the shipped APK, without ads and analytics

## Status

Accepted (recorded 2026-10-23, describes the existing codebase)

## Context

The product existed only as a shipped, obfuscated APK (14.7.4) built on a stack of ad mediation, analytics,
the ikame billing wrapper, EventBus/RxJava, Room and a bundled MJPEG module. We need a maintainable source tree
with the same user-facing behaviour.

## Decision

- Rebuild the app in Kotlin (`:app`), one Activity per screen, ViewBinding, singletons for shared state.
- Keep the customized ConnectSDK as a Java library module (`:connectsdk`), upstream sources where unchanged,
  APK-specific services ported from the decompile.
- Keep resources, assets, prefs file and key names, product IDs, ports and protocol behaviour identical.
- Remove ads and analytics SDKs entirely.
- Replace wrappers with direct, smaller pieces: Play Billing (`data/Premium.kt`), Flows instead of EventBus/Rx,
  JSON-in-prefs instead of Room for browser history, own MediaProjection + MJPEG server, `dadb` for Fire TV ADB.
- Name the obfuscated original in KDoc (`Port of \`rm5\``) for traceability.

## Alternatives Considered

1. Patch the decompiled smali/Java directly — unmaintainable, keeps ad SDKs.
2. Rewrite from scratch with a new design (Compose, MVVM) — loses parity, much larger.
3. Port, keep the original architecture but drop monetization SDKs — chosen.

## Consequences

### Positive

- Readable code with traceable origins; much smaller dependency surface; no ad/tracking SDKs.

### Negative

- Global mutable singletons and Activity-heavy code inherited from the original.
- No tests carried over; resource lint debt carried over (baselined).
- Revenue from ads is gone; premium only.

### Follow-up

- Unit tests for pure logic (see `docs/validation/test-matrix.md`).
- Tech debt items TD-001…TD-008.
