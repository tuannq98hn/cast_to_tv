# RELIABILITY.md

This document defines reliability expectations.

## Reliability Principles

- Fail safely.
- Make errors observable.
- Avoid silent data loss.
- Prefer idempotent operations where possible.
- Handle offline, retry, timeout, and partial failure scenarios explicitly.

## Project-Specific Expectations

- Never do network or file I/O on the main thread (TV calls, MediaStore queries, local servers).
- Wi-Fi changes: discovery restarts and the UI reflects a dropped connection (`TvConnect.events` DISCONNECT).
- A TV that stops answering must not hang a screen: use timeouts on OkHttp/socket calls.
- Local servers (FileServer, MjpegServer, WebReceiver) must free their ports when the feature stops; a second start
  must not crash on "address in use".
- Mirroring runs as a `mediaProjection` foreground service with a notification; stopping it from the notification
  or the app must release the projection.
- Billing: losing the billing connection keeps the cached entitlement; reconnect on next use.

## Required Reliability Checks

- Critical flows (connect, cast photo, cast video, mirror, remote keys) have tests or manual verification notes
  in `docs/validation/test-matrix.md`.
- Network calls have timeout/error behavior.
- User-facing errors are understandable.

## Incident Notes

Add incident summaries and follow-up actions here.
