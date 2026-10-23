# Product Spec: New User Onboarding

## Status

Implemented — parity with the shipped app (`ui/activities/splash/Onboarding.kt`).

## Purpose

Get a first-time user to the home screen quickly, in their language, and show the purchase screen only to the
user tier configured for it.

## Flow

After `SplashActivity`, `Onboarding.afterSplash` picks the next screen:

| Condition | Next screen |
|---|---|
| First run (`KEY_OPEN_APP` == 0) and `language_first` | `LanguageFirstActivity` |
| First run, no purchase required | `MainActivity` |
| First run, purchase required, `new_intro` | `NewIntroActivity` |
| Purchase required (`user_tier2` and network not in list) and not premium | `PurchaseActivity` (from `screen_splash`) |
| Otherwise | `MainActivity` |

After the language screen (`Onboarding.afterLanguage`): no purchase required or premium → `IntroActivity` if
`enable_intro` else `MainActivity`; otherwise `NewIntroActivity` if `new_intro`, else `PurchaseActivity`.

## Rules

- Do not ask for unnecessary permissions too early: media/camera/mic permissions are requested by the feature
  that needs them. Notifications (Android 13+) are asked on `MainActivity` after an explainer, not during the
  onboarding screens.
- Explain value before asking for sensitive access.
- The purchase screen must always be closable.
- Make skip/back behavior explicit.

## Acceptance Criteria

- Fresh install with default prefs: Splash → Language → Main.
- Premium user never sees `PurchaseActivity` from onboarding.
- Second launch goes straight to Main.

## Test Expectations

`Onboarding` branching is pure given `AppPrefs` + `Premium.isActive`; cover the table above with unit tests
when the code is next changed.
