# FRONTEND.md

This document defines frontend implementation guidance for web, mobile, and desktop clients.

## UI Layer Rules

- UI should render state and emit user intent.
- UI should not own business logic that belongs in application/domain layers.
- UI should not directly call APIs, databases, SDKs, or low-level services unless explicitly approved by architecture.
- Complex widgets/components should be decomposed into smaller reusable units.

## State Management

- One Activity per screen extending `base/BaseActivity`; layouts in `res/layout`, accessed through ViewBinding.
  No Compose, no ViewModel, no DI.
- Shared state lives in singletons: `TvConnect` (connected device, `events` SharedFlow), `Premium.active`
  (StateFlow), `BrowserHistory.items` (StateFlow), `AppPrefs`. Collect flows with
  `lifecycleScope` + `repeatOnLifecycle(STARTED)`.
- Async: coroutines (`lifecycleScope`, `Dispatchers.IO`) or `thread {}` for blocking protocol code; post results
  back to the main thread before touching views.
- Navigation: `BaseActivity.open(Class) { extras }`. Features that need a TV go through the "connect first"
  launcher in `BaseActivity`, which opens `SearchTVActivity` and continues the action on `RESULT_OK`.
- Sizes use `sdp`/`ssp` dimensions (`@dimen/_12sdp`) like the shipped layouts; keep using them for parity.
- Strings: every user-visible string in `res/values/values.xml` and translated in `values-*` (90+ locales).
  Add new strings to `values/` at least; missing translations are lint warnings already baselined.

## Testing Expectations

- Pure logic (URL building, device classification, protocol message encoding) gets JVM unit tests in
  `app/src/test`.
- Screens: verify manually on a device or emulator; every Activity is exported in debug builds, so
  `adb shell am start -n com.casttotv.screenmirroring.smarttv.castvideo/com.casttotv.screen.mirroring.ui.activities.main.MainActivity`
  opens a screen directly for screenshots (see the `ui-evidence` skill).
- Document manual UI verification in the task summary when automated tests are not available.
