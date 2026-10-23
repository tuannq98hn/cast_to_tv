# Architecture Rules

This file defines architecture constraints that should be enforced by review, tests, scripts, or CI when possible.

## General Rules

- Keep business/domain logic independent from UI and infrastructure.
- Keep platform-specific integrations behind adapters/services.
- Avoid circular dependencies.
- Avoid large files with mixed responsibilities.
- Public APIs/contracts should be documented.
- Data models should be explicit and versioned when needed.

## Import Boundary Rules

```txt
ui      may import base, tv, mirror, data, models, connectsdk types
tv      may import connectsdk, data, models — never ui
mirror  may import tv, data — never ui
data    may import models — never ui
connectsdk must not import com.casttotv.*
```

## Enforced by `scripts/ci/verify_architecture.sh`

| Rule | Why |
|---|---|
| `com.android.billingclient` only in `data/Premium.kt` | One place decides premium; screens read `Premium.active` |
| No ads/analytics SDK imports (Firebase, AdMob, AppLovin, Facebook Ads, AppsFlyer, Adjust, ikame) | Removed in the port (ADR-0001); bringing one back needs a new ADR + SECURITY/Data-safety update |
| `connectsdk` never imports app code | Library module; Gradle would also fail, the check gives a clearer message |
| `ui/` does not import OkHttp / Java-WebSocket / socket.io / dadb / raw sockets | Protocol code belongs in `tv/` or `mirror/`. `ChannelFragment.kt` is grandfathered (TD-004) |
| No keystores / `.env` / keys tracked by git | Secrets |

## Review-only Rules

- New local server ports or endpoints: update the port table in `ARCHITECTURE.md`.
- New permission in `AndroidManifest.xml`: say which feature requests it and when (runtime prompt timing).
- New prefs key: add it to `docs/generated/db-schema.md`; keep shipped key names unchanged.
- Keep `tv/` and `mirror/` free of Android `View` / Activity references (Context is fine).

## Enforcement

Add project-specific checks to `scripts/ci/verify_architecture.sh`.
