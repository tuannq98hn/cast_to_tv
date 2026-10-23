# ARCHITECTURE.md

This document describes the system architecture at a level that helps humans and AI agents make safe changes.

## Architecture Summary

- Product type: Android phone app — cast local media / web video to a TV, mirror the screen, act as a TV remote.
- Origin: a Kotlin rebuild of the shipped APK `com.casttotv.screenmirroring.smarttv.castvideo` 14.7.4
  (see ADR-0001). Many KDoc comments name the obfuscated original (`Port of \`rm5\``); keep them, they map code
  back to the decompile.
- Platform: Android only, minSdk 24, target/compile SDK 35, portrait-only Activities.
- Language: Kotlin (app), Java (connectsdk + two vendored libraries), JDK 17, Gradle Kotlin DSL, AGP 8.13.
- UI: classic Views + ViewBinding, one Activity per screen, a few Fragments (remotes). No Compose, no DI framework,
  no ViewModel layer.
- Data storage: SharedPreferences only (see `docs/generated/db-schema.md`). No database, no backend of our own.
- External services: Google Play Billing; `bcastmirror.com` socket.io relay (web mirroring); Google suggest API
  (browser search box); YouTube pages (via `at.huber.youtubeExtractor`). Ads and analytics were removed in the port.

## Modules

| Module | What it is |
|---|---|
| `:app` | The application (`com.casttotv.screen.mirroring`). Also vendors `at.huber.youtubeExtractor` and `com.evgenii.jsevaluator` as plain sources. |
| `:connectsdk` | Customized ConnectSDK fork ("inshot cast core"): device discovery (SSDP, mDNS, Cast) and services for webOS, NetCast, Roku, DIAL, DLNA/UPnP, Google Cast, AirPlay, Vizio, Fire TV (`libs/AmazonFling.jar`, `libs/WhisperPlay.jar`). Must not depend on `:app`. |

## System Boundaries (`:app`)

| Package | Responsibility | Must Not Do |
|---|---|---|
| `ui/activities/**`, `ui/dialogs`, `ui/view`, `customview` | Screens: render state, collect intent, start other screens | Open sockets / HTTP clients directly (use `tv/`), touch BillingClient |
| `base/BaseActivity` | System bars, `open()`, "connect first, then continue" flow, disconnect dialog | Hold feature logic |
| `tv/` | Connection state (`TvConnect`) and per-brand protocol clients | Depend on `ui/` |
| `mirror/` | Screen capture (`MirrorService`, MediaProjection), MJPEG HTTP server, pushing the stream to a TV or the web relay | Depend on `ui/` |
| `data/` | `AppPrefs` (prefs keys of the shipped app), `Premium` (Play Billing + cached entitlement), `MediaStoreRepo`, `BrowserHistory` | Depend on `ui/` |
| `models/` | Plain data classes (`MediaItem`, `PhotoAlbum`, …) | Contain logic with side effects |
| `service/` | `RemindAlarmReceiver` (reminder notification) | — |

## Dependency Direction

```txt
ui -> base, tv, mirror, data, models
mirror -> tv, data
tv -> connectsdk, data
data -> models
connectsdk -> (nothing in :app)
```

Enforced by `scripts/ci/verify_architecture.sh` where a grep can see it (see `docs/validation/architecture-rules.md`).

## Startup and Data Flow

```txt
MyApplication.onCreate
  -> AppPrefs.init, BrowserHistory.init, Premium.init (connects BillingClient), SamsungRemote.init
  -> DiscoveryManager.init + start (connectsdk, keeps scanning the LAN)
  -> DeviceConnectService.enqueueWork

SplashActivity -> Onboarding.afterSplash -> [LanguageFirst] -> [Intro/NewIntro] -> [Purchase] -> MainActivity
```

Casting a file:

```txt
Screen -> BaseActivity "connect first" (SearchTVActivity, result OK) -> TvConnect.device
       -> tv/FileServer serves the file at http://<phone-ip>:8093<path> (Range support)
       -> TvConnect.castDevice MediaPlayer capability (connectsdk) plays the URL on the TV
```

Connection events are a `SharedFlow` (`TvConnect.events`: CONNECT/DISCONNECT); screens collect it with
`repeatOnLifecycle`. Billing state is a `StateFlow` (`Premium.active`).

## Network Endpoints

The phone is both client and server on the LAN. Keep these ports stable; TV-side pages and apps depend on them.

| Where | Port | Purpose |
|---|---|---|
| `tv/FileServer` (phone) | 8093, else random 8000–8998 | Serves local media to the TV |
| `mirror/MjpegServer` (phone) | 8080 | `/` viewer page, `/stream.mjpeg` screen stream |
| `tv/WebReceiver` (phone) | HTTP 8686, WS 8268 | "Web cast": TV browser opens the page in `assets/web`, app drives its `<video>` |
| Samsung Tizen (TV) | 8002 wss (token) / 8001 ws | `samsung.remote.control` channel |
| Android TV (TV) | 6467 pairing, 6466 keys | Android TV Remote protocol v2, TLS with app-generated cert (BouncyCastle) |
| Roku (TV) | 8060 | External Control Protocol |
| Fire TV (TV) | 5555 | `adb shell` over Wi-Fi (`dadb`) |
| Sony Bravia (TV) | 80 | `/sony/accessControl`, `/sony/IRCC` |
| `bcastmirror.com` | 443 | socket.io relay for mirroring to a web page scanned by QR |

## Error Handling Policy

- TV protocol calls fail often (TV asleep, wrong network, pairing refused). Show a user-facing state
  (connect-fail animation, toast, pairing dialog) instead of crashing; never block the main thread on I/O.
- Do not log pairing codes, Samsung tokens, or purchase tokens.
- Billing errors leave the cached entitlement unchanged; only a successful query/purchase changes it.

## Configuration Policy

- No secrets and no build-time env vars today. Product IDs live in `data/Premium.kt`, remote-config-like flags in
  `AppPrefs` with the shipped app's default values.
- `app/src/debug/AndroidManifest.xml` exports every Activity and the reminder receiver so `adb shell am start`
  can open any screen for testing and screenshots. Release builds keep `exported="false"`.
- Lint runs against `app/lint-baseline.xml` (inherited resource issues). New lint errors fail the gate.

## Architecture Change Policy

When making architecture-level changes:

1. Add or update an ADR under `docs/decisions/`.
2. Update this file.
3. Update validation rules if needed.
4. Run the full quality gate.
