# Environment Reference

There are no runtime environment variables or secrets today. What a build needs:

| Name | Required | Scope | Description | Example |
|---|---|---|---|---|
| `sdk.dir` in `local.properties` | Yes (or `ANDROID_HOME`) | local | Android SDK path; untracked, `bootstrap.sh` writes it | `/Users/me/Library/Android/sdk` |
| JDK | Yes | local/CI | JDK 17+ (toolchain 17) | Android Studio JBR 21 works |
| `ANDROID_HOME` | No | local/CI | Used by `bootstrap.sh` when `local.properties` is missing | |
| Release keystore | Release only | CI secret | Not configured in Gradle yet; add `signingConfigs` reading from env/CI, never commit the file | |

## In-code configuration (not env)

| Where | What |
|---|---|
| `app/build.gradle.kts` | `applicationId` `com.casttotv.screenmirroring.smarttv.castvideo`, `versionCode`/`versionName` |
| `data/Premium.kt` | Play Billing product IDs (`cgt_*_notrial_s1`, legacy IDs still count as premium) |
| `data/AppPrefs.kt` | Flags of the shipped app's remote config (`language_first`, `new_intro`, `user_tier2`, …) with their defaults |

## Rules

- Never include real secrets.
- Use fake examples only.
- Mention where each value should be configured: local `local.properties`, CI secret, etc.
