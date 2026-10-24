# Tech Debt Tracker

Use this file to track known debt that is not fixed immediately.

| ID | Area | Description | Impact | Priority | Owner | Status |
|---|---|---|---|---|---|---|
| TD-001 | Tests | No unit or instrumented tests. Start with pure logic: `Onboarding`, `FileServer` Range parsing, Roku/Sony/Samsung message building, `BrowserHistory.siteOf` | Regressions only found on devices | P1 | — | Open |
| TD-002 | Lint | 406 errors / 1417 warnings grandfathered in `app/lint-baseline.xml` (mostly MissingTranslation, unused resources, 3 StringFormatMatches) | Noise; real format-string bugs in some locales | P2 | — | Open |
| TD-003 | Security | Local HTTP servers (8080, 8093, 8686/8268) serve without auth to the whole LAN | Same-Wi-Fi users can view stream/files while active | P2 | — | Open |
| TD-004 | Architecture | `ui/.../remote/ChannelFragment.kt` uses OkHttp directly; move to `tv/` then drop it from the allow-list in `verify_architecture.sh` | Boundary leak | P3 | — | Open |
| TD-005 | Security | `AndroidTvRemote` trusts all server certs; `WebCastActivity` offers "continue" on SSL errors; global cleartext | MITM on LAN / web | P2 | — | Open (parity) |
| TD-006 | Architecture | `TvConnect` is global mutable state (`var device`, etc.) touched from many threads | Race-y connect/disconnect | P2 | — | Open |
| TD-007 | Build | Release: `isMinifyEnabled = false`, no signing config | Larger APK, release not reproducible from repo | P2 | — | Open |
| TD-008 | Platform | `MediaStoreRepo` reads deprecated `MediaStore.*.DATA` file paths | May break on future Android versions / scoped storage | P3 | — | Open |
| TD-009 | Harness | `.agents/skills/` (11 harness skills) missing from this copy of the template | Roles reference skills that do not exist | P2 | — | Closed 2026-10-23 (skills restored) |
| TD-010 | CI | No CI runs the quality gate | Gate only runs in the agent loop | P3 | — | Open |
| TD-011 | Billing | `PurchaseActivity` "More options" Monthly/Lifetime cards (`layout_options.xml`) have no click handler and no price; only the yearly CTA buys | Users can't pick monthly/lifetime | P2 | — | Open |
| TD-011 | Resources | Dark Cinema restyle left legacy drawables/raws possibly unused (e.g. `raw/video_premium_1`, `raw/audio_intro`, `bg_header_miror`, `img_touchpad_ss*`, `img_setting`); full list per task in the plan's Progress Log "may now be unused" notes | APK size, lint noise | P3 | — | Open |

## Rules

- Do not hide known issues in chat history.
- Add debt when a shortcut is taken.
- Link debt to execution plans or bugs when possible.
- Review this file before starting a new sprint or release.
