# PRODUCT_SENSE.md

This document captures product judgment that should guide implementation.

## Product Goal

Put what is on the phone onto the TV with as few steps as possible: photos, videos, music, a web video, or the whole
screen. And replace a lost TV remote. The app works on the local Wi-Fi without an account.

## Target Users

- Primary: non-technical smart-TV owners (Samsung, LG, Roku, Sony, Android TV / Google TV, Fire TV, Chromecast,
  DLNA TVs) who want to show phone content on the big screen.
- Secondary: people whose TV brand is not supported natively — they use "web cast" / web mirroring through the
  TV's browser.
- Context: living room, phone and TV on the same Wi-Fi, often a TV with confusing pairing prompts.

## Features (current scope)

| Feature | Screens |
|---|---|
| Connect to a TV | `SearchTVActivity`, pairing dialogs, `GuideFireTVActivity`, `InstallChannelRokuActivity` |
| Cast photos / videos / audio | `PhotoOfflineActivity`, `MediaActivity`, `DetailMediaActivity`, `CastMediaActivity`, `FileBrowserActivity`, share-to-app (`MediaActionActivity`) |
| Cast web video | `WebCastActivity`, `HistoryBrowserActivity`, `YoutubeBrowserActivity` |
| Screen mirroring | `ScreenMirroringActivity` (to connected TV), `QrScanActivity` (to a web page via bcastmirror.com) |
| TV remote | `RemoteActivity` + one fragment per brand, channel list |
| Premium | `PurchaseActivity`, `SubSaleActivity` (Play Billing subscriptions + lifetime) |
| Onboarding / misc | Splash, language, intro, how-to, help, feedback, settings, rate dialog |

## Product Principles

- Optimize for user value, not only feature count.
- Avoid adding flows that are not supported by product specs.
- Behaviour parity with the shipped app is the default (ADR-0001); deviations are documented, not accidental.
- Connecting must explain itself: what to accept on the TV, what to do when the TV is not found.
- Make monetization, permissions, payments, and account behavior transparent. Ask for camera, media and mic
  permissions only when the feature that needs them is opened. Exception (parity): on Android 13+
  `MainActivity` shows an explainer and asks for notifications on first reach of the home screen.
- Respect platform policies and user trust (Play Billing rules, foreground-service type for mirroring).

## Decision Questions

Before adding or changing behavior, ask:

1. Which user problem does this solve?
2. Is this in scope for the current spec?
3. What could confuse the user?
4. What could break existing behavior — on which TV brands?
5. How will success be measured?
