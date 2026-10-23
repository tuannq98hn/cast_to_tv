# Local Storage Schema

Manually maintained (no database). Everything persistent is SharedPreferences plus one keystore file.
Key names come from the shipped app — do not rename them, existing installs depend on them.

## Generation Command

```bash
# Re-check against the code:
grep -rhoE '(get|put)(String|Boolean|Int|Long|Float)\("[^"]+"' app/src/main/java | sort -u
```

## SharedPreferences files

| File | Key | Type | Default | Owner / meaning |
|---|---|---|---|---|
| `app-content` | `key_language` | String | `en` | `AppPrefs.language` |
| | `language_order`, `auto_select_language` | String / Boolean | `""` / false | Language screen ordering |
| | `first_open`, `KEY_OPEN_APP`, `KEY_DATE_START_APP` | Boolean / Int / String | true / 0 / null | Launch counters (onboarding, rate dialog) |
| | `language_first`, `enable_intro`, `new_intro` | Boolean | true / false / false | Onboarding flags (remote-config values of the shipped app) |
| | `hide_bottom_navigation_bar` | Boolean | true | `BaseActivity` hides the nav bar |
| | `user_tier2`, `list_type_network`, `type_network` | Boolean / String / String | false / `"applovin, unity, tiktok, minteral"` / `"null"` | `AppPrefs.requirePurchaseForFeatures` |
| | `is_premium` | Boolean | false | `Premium` cached entitlement |
| | `FIRST_FIRETV` | Boolean | false | Fire TV guide shown once |
| `settings` | `haptic_feedback` | Boolean | true | Settings "Vibrate" switch |
| `data` | `rate`, `count_rate` | Boolean / Int | false / 3 | Rate dialog |
| `browser_history` | `history_browser` | String (JSON array of `{id,title,linkUrl,type,date}`) | `[]` | `BrowserHistory` |
| `SamsungATV` | `TOKEN_SS_REMOTE`, `IP_SAMSUNG_REMOTE` | String | — | Samsung pairing token + last TV IP (sensitive) |

## Files

| Path | What |
|---|---|
| `filesDir/atvremote.p12` | Android TV Remote client key + self-signed cert (PKCS12, empty password). Sensitive: the TV trusts it after pairing. |

`allowBackup="false"` — nothing above leaves the device through Android backup.
