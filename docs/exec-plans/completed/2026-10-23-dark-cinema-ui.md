# Execution Plan: Dark Cinema UI redesign

## Status

Completed (2026-10-23)

## Owner

Mixed

## Problem

Move the whole app UI to the Figma design `CastToTV` / `Page 1` ("Dark Cinema": dark glass cards, cyan + violet accents).
**Behavior stays exactly the same**: no changes to `tv/`, `mirror/`, `data/`, `connectsdk`, routing, billing, or prefs.
The Figma file replaces APK 14.7.4 as the design source (`docs/DESIGN.md` updated in T15, ADR-0002).

Decisions (2026-10-23, user):
- Screens without a Figma frame are self-styled in Dark Cinema using the T1 tokens/components ("self-styled" below).
- Design elements that need behavior the app does not have are **left out** (see "Not built").
- Remote "Number" tab: no design → kept as an extra tab, only visible when it is today (connected, not Roku/Fire TV).

## Design references

Everything an agent needs is in the repo; **agents do not need Figma/MCP access**:

- `docs/references/figma-dark-cinema/<frame>.png` — 1x screenshot of each frame.
- `docs/references/figma-dark-cinema/nodes/<frame>.json` — full node tree of each frame (bounds, auto-layout padding/spacing,
  corner radius, fills/strokes/gradients/effects, text content + font size/weight/line height). Style refs are resolved inline.
- `docs/references/figma-dark-cinema/tokens.md` — generated summary: most-used colors, text colors, text styles, radii, gradients.
  T1 picks its tokens from here.

Re-export (humans, Figma desktop open): figma-mcp-go `get_nodes_info` per frame (the plugin truncates at ~500 nodes per call,
so fetch 1–2 frames at a time).

| Figma frame | Node | PNG |
|---|---|---|
| Main Hub | `1:2` | `main-hub.png` |
| PRO Upgrade | `1:175` | `pro-upgrade.png` |
| Smart Remote | `1:332` | `smart-remote.png` |
| Photo Cast | `1:487` | `photo-cast.png` |
| Video Hub | `1:656` | `video-hub.png` |
| Cast Player | `1:922` | `cast-player.png` |
| Connect Devices | `1:1073` | `connect-devices.png` |
| Web Video Cast | `1:1339` | `web-video-cast.png` |
| Screen Mirroring | `1:1668` | `screen-mirroring.png` |
| Exit App Dialog | `1:1908` | `exit-dialog.png` |
| Rate App Dialog | `1:2034` | `rate-dialog.png` |
| Notification Sheet | `1:2160` | `notification-sheet.png` |
| Onboarding Intro | `1:2283` | `onboarding-intro.png` |
| Language Selection | `1:2369` | `language-selection.png` |
| Splash Screen | `1:2560` | `splash.png` |
| Help & FAQ | `1:2658` | `help-faq.png` |
| Feedback | `1:2928` | `feedback.png` |
| Settings | `1:3093` | `settings.png` |

Current-app screenshots (numbered 01–39) are in `CastToTV-screenshots/`.

## Screen coverage (39 screens)

| # | Screen | Figma | Code | Task |
|---|---|---|---|---|
| 01 | Splash | Splash Screen | `SplashActivity` | T12 |
| 02 | Language First | Language Selection | `LanguageFirstActivity` | T12 |
| 03 | Intro | Onboarding (1 slide only) → reuse for all slides | `IntroActivity` | T12 |
| 04 | NewIntro | Onboarding Intro | `NewIntroActivity` | T12 |
| 05 | Main | Main Hub | `MainActivity` | T2 |
| 06 | Connect SearchTV | Connect Devices | `SearchTVActivity`, `DeviceAdapter` | T9 |
| 07 | Photo | Photo Cast | `PhotoOfflineActivity` | T5 |
| 08 | Video Media | Video Hub | `MediaActivity` | T6 |
| 09 | Screen Mirroring | Screen Mirroring | `ScreenMirroringActivity` + mirror fragments | T10 |
| 10 | QR Scan | self-styled | `QrScanActivity` | T10 |
| 11 | Remote | Smart Remote | `RemoteActivity`, `RokuRemoteFragment` | T4a, T4b |
| 12 | Guide FireTV | self-styled | `GuideFireTVActivity` | T4c |
| 13 | YouTube Browser | self-styled (chrome only) | `YoutubeBrowserActivity` | T11 |
| 14 | Vimeo Browser | self-styled (chrome only) | `YoutubeBrowserActivity` | T11 |
| 15 | Web Cast | Web Video Cast | `WebCastActivity` | T11 |
| 16 | Web Cast History | partial ("Recent Sessions" rows) | `HistoryBrowserActivity` | T11 |
| 17 | File Browser TV | self-styled | `FileBrowserActivity` | T11 |
| 18 | Settings | Settings | `SettingActivity` | T13 |
| 19 | Language From Settings | Language Selection | `LanguageFirstActivity` | T12 |
| 20 | How To Connect TV | self-styled | `HowToUseActivity` (type 1) | T13 |
| 21 | Feedback | Feedback | `FeedbackActivity` | T13 |
| 22 | Help | Help & FAQ | `HelpActivity` | T13 |
| 23 | Purchase Premium | PRO Upgrade | `PurchaseActivity` | T3 |
| 24 | SubSale | self-styled (from PRO Upgrade) | `SubSaleActivity` | T3 |
| 25 | Install Channel Roku | self-styled | `InstallChannelRokuActivity` | T9 |
| 26 | Share Cast Photo | self-styled (from Cast Player) | `MediaActionActivity` | T7 |
| 27 | Share Cast Video | self-styled (from Cast Player) | `MediaActionActivity` | T7 |
| 28 | Cast Player Buffering | Cast Player | `CastMediaActivity` | T7 |
| 29 | Video Album Tab | partial ("Album Folders" tab) | `MediaActivity` | T6 |
| 30 | Video Album Detail | self-styled (from Video Hub) | `DetailMediaActivity` | T6 |
| 31 | How To Cast | self-styled | `HowToUseActivity` (type 2) | T13 |
| 32 | How To Screen Mirror | self-styled | `HowToUseActivity` (type 3) | T13 |
| 33 | Remote Touchpad | partial (tab only) | `RemoteTouch` | T4c |
| 34 | Remote Channels | partial (tab only) | `ChannelFragment` | T4c |
| 35 | Rate Dialog | Rate App Dialog | `RateDialog` | T14 |
| 36 | Exit Dialog | Exit App Dialog | `PairingDialogs.kt` (`ExitDialog`) | T14 |
| 37 | Notification Permission Sheet | Notification Sheet | `PermissionSheet` | T14 |
| 38 | Purchase More Options | partial (collapsed only) | `PurchaseActivity` (`expandable`) | T3 |
| 39 | Reminder Notification | self-styled | `RemindAlarmReceiver` | T14 |

## Not built (design shows it, app has no such behavior)

- Bottom navigation bar (Media / Remote / Mirror / Devices / Settings / Apps) on every screen.
- Photo/Video: multi-select, Select All, Slideshow, Queue Video, "Selected" bar, sort menu, 4K/HDR/Dolby badges unless `MediaItem` has the data.
- Web Video Cast: Twitch, TikTok, IMDb, LAN Cast hubs, featured video card, "Cast Sniffer Engine", Saved Streams tab.
- Screen Mirroring: Miracast mode, Stream Tuning (resolution/FPS), Spatial Audio toggle, latency/Wi-Fi stats.
- Connect Devices: radar animation is fine (decoration), but network name/band, per-device protocol tags and signal icons only if `TvConnect`/`DeviceAdapter` already has the data.
- Cast Player: CC, aspect ratio, Dolby, bitrate stats, "Switch TV" (unless it maps to the existing connect flow), repeat if not present.
- Settings: search button, Auto-Connect toggle, "3-Day Free Trial" banner wording.
- Profile avatar button in headers.
- Placeholder copy from the design is never shipped: "AetherCast v3.4.2", "App Store", "$19.99/yr", fake device/video names. Use the existing strings, `Premium.price()`, and `BuildConfig.VERSION_NAME`.
- Fonts: the frames use "Liberation Sans" (an import fallback) → use the existing `pro_display_*` fonts.

## Implementation rules (every task)

> **UI ONLY — DO NOT CHANGE FUNCTIONAL LOGIC.** Do not touch `tv/`, `mirror/`, `data/`, `models/`, `service/` logic,
> `connectsdk`, `BaseActivity` flows, navigation/routing, billing, permissions, prefs, or casting/remote commands.
> Kotlin edits are allowed only to bind/remove views (e.g. dropping the PRO promo video, renamed IDs, applying styles).
> If a design element needs new behavior, leave it out and add a task note; if the UI cannot be done without a logic
> change, `task block` with the reason instead of changing it.

- Change layouts and drawables; Kotlin only where a view is removed or renamed. Keep view IDs so click handlers stay.
- `sdp`/`ssp` dimensions, every new string in `res/values/values.xml`.
- No real blur below API 31: use translucent gradient + 1dp stroke.
- Accept criteria for every screen task: every click handler and state (connected / not connected, empty, loading, premium / free)
  behaves as before; screenshot compared with the Figma PNG (or with T1 components when self-styled), attached to the task summary.

## Task list

| Task | Scope | Screens | Prio | Deps |
|---|---|---|---|---|
| T1 | Foundation: color tokens from `globalVars.styles`, `Theme.CastTV` → dark, shared drawables (glass card, chip, icon tile, gradient CTA, key, dialog/sheet bg), text styles, shared header layout (`include` used by `bindHeader`) | — | P0 | — |
| T2 | Main Hub | 05 | P1 | T1 |
| T3 | Purchase + SubSale: drop video/music/animations, glass sheet, Restore → `Premium.restore()`, expanded "More options" self-styled | 23, 38, 24 | P1 | T1 |
| T4a | Remote shell: header, segmented tabs (D-Pad / Touch / Apps (+ Number)) | 11 | P1 | T1 |
| T4b | Remote keys + Roku pad | 11 | P1 | T1 |
| T4c | Other brand remotes (Samsung, LG, Sony, Android TV, Fire TV), touchpad, channels, number pad, keyboard/connect dialogs, Guide FireTV | 12, 33, 34 | P2 | T4b |
| T5 | Photo Cast | 07 | P1 | T1 |
| T6 | Video Hub + album tab + album detail | 08, 29, 30 | P1 | T1 |
| T7 | Cast Player + Share Cast Photo/Video + casting dialog/bottom sheet | 28, 26, 27 | P1 | T1 |
| T9 | Connect Devices: SearchTV, device item, Wi-Fi off dialog, pairing / PIN / disconnect dialogs, Samsung check dialog, Install Channel Roku | 06, 25 | P1 | T1 |
| T10 | Screen Mirroring (+ smart/direct/web/FAQ fragments) + QR Scan | 09, 10 | P1 | T1 |
| T11 | Web Cast + History + YouTube/Vimeo browser chrome + File Browser + their dialogs | 15, 16, 13, 14, 17 | P2 | T1 |
| T12 | Splash, Language (first + from settings), Intro, NewIntro | 01, 02, 19, 03, 04 | P2 | T1 |
| T13 | Settings, Help & FAQ, Feedback, How To (3 types) | 18, 22, 21, 20, 31, 32 | P2 | T1 |
| T14 | Rate dialog, Exit dialog, Permission sheet (all types), Reminder notification | 35, 36, 37, 39 | P2 | T1 |
| T15 | Docs: `DESIGN.md` source of truth → Figma, ADR, move plan to `completed/` | — | P3 | all |

## Board Tasks

T1 = T-001 · T2 = T-002 · T3 = T-003 · T4a = T-004 · T4b = T-005 · T4c = T-006 · T5 = T-007 · T6 = T-008 ·
T7 = T-009 · T9 = T-010 · T10 = T-011 · T11 = T-012 · T12 = T-013 · T13 = T-014 · T14 = T-015 · T15 = T-016

## Validation Plan

- [x] `bash scripts/agent/run_quality_gate.sh` per task
- [x] Screenshot of each screen via `adb shell am start` vs the Figma PNG (ui-evidence skill) — partial, see Completion Notes
- [ ] Regression: walk every click handler of the touched screen
- [ ] Real-TV smoke test of remotes (per brand available) after T4b/T4c — **not done**
- [x] Docs updated (T15)

## Risks

| Risk | Mitigation |
|---|---|
| Light and dark screens mixed until all tasks land | T1 switches the global theme; screens look "dark but old" until their task lands |
| Remote layouts are large; key mappings easy to break | Style only, keep IDs; T4b per brand smoke test |
| Removing PRO promo video/music may change conversion | Product decision; called out in the T3 PR |
| Designers may later send frames for self-styled screens | Self-styled screens use only T1 components, so redoing them stays cheap |

## Progress Log

- 2026-10-23: Created plan; exported 4 Figma frames.
- 2026-10-23: Figma extended to 18 frames; mapped all 39 screens; user chose self-styled Dark Cinema for screens without a frame.
- 2026-10-23: T1 — `dc_*` colors, `DC.Text.*` styles, dark `Theme.CastTV`, `dc_bg_*` drawables, `item_header` restyle; tokens in `docs/references/design-system-reference-llms.txt`.
- 2026-10-23: T2 — Main Hub restyled (`activity_main.xml`, `dc_bg_hero_mirror`, `dc_bg_icon_tile_tint`, `dc_bg_chevron`); IDs kept, no Kotlin change. Left out: card subtitles, HIGH SPEED chip, "All Formats" (new untranslated copy / no behavior).
- 2026-10-23: T3 — Purchase + SubSale restyled (`activity_premium_old`, `activity_sale1`, shared `layout_pro_features`, option rows, `dialog_sale`); promo video, music, shine/shake animations and `BottomSheetBehavior` removed; "Restore Purchase" → `Premium.restore()` (new string `restore_purchase`, English only). Left out: hero TV/phone illustration (no asset), "PREMIUM UPGRADE" chip, subtitle/feature subtitles, "Just ~/month" line. `raw/video_premium_1`, `raw/audio_intro`, `anim/left_right|shake` may now be unused (not deleted).
- 2026-10-23: T4a — Remote shell: `activity_remote.xml` tab bar → Figma segmented control (`bg_tabar_remote`, `bg_item_tab` restyled, `ic_tab_*_on/off` recolored cyan/muted), labels D-Pad/Touch/Number/Apps (`remote_tab_*`, English only, `color/dc_tab_label`), `RemoteActivity.select()` sets `isSelected` for label color only. Left out: header subtitle (device name line) + cyan cast button styling (shared header, needs new binding). `bg_background_main` may now be unused (not deleted).
- 2026-10-23: T4b — Roku remote keys: shared `DC.Remote.Key/.Power/KeyIcon/.Accent/Crown/OkLabel` styles + `dc_bg_key_power`, `dc_bg_crown`; `bg_control*` PNGs (Roku-only) replaced by same-named vector/layer-list drawables (cross, rings, violet OK, chevrons; pressed arm = rotated `dc_dpad_press_up`, `dc_dpad_press_ok`) so `bindPressedBackground` is untouched; `fragment_remote.xml` restyled, D-pad guidelines 0.345/0.655, OK label added (`@string/ok`); IDs and key commands unchanged, no Kotlin change. Left out: drop shadows/glows except a radial OK glow (no blur).
- 2026-10-23: T5 — Photo Cast: `activity_photo_offline` dark (dc_bg, dc_divider, cyan progress, muted empty text), album chips → pills (`dc_bg_chip` / new `dc_bg_chip_active` gradient #3131c0→#00f2fe, `dc_chip_active_start`), `layout_item_photos` → 4:5 card r12 on dc_surface_raised + glass stroke; Kotlin: chip drawable/text colors + grid span 3→2 only. `fragment_media` untouched (shared with MediaActivity). Left out: chip icon/count, card name/time/size, 4K/HDR badges, select ring, Today subheader, Select All/grid toggle, selection bar/Slideshow/Cast to TV, device status chip, bottom nav. `bg_border20_color_main` may now be unused (not deleted).
- 2026-10-23: T6 — Video Hub: `activity_list_media` TabLayout → Figma segmented pill (`dc_bg_segment_track`, `bg_tab_layout` + `TabLayoutTextStyle` restyled in place, both only used here); `layout_item_video` / `item_album_photo` → media card (`dc_bg_media_card` #1b2029b2 + glass stroke, rounded 4:3 / 5:4 thumb, `dc_media_scrim_bottom`, `dc_bg_media_badge` duration/count pill with cyan icon, title now visible); `fragment_media_all` empty/loading tints; `activity_detail_media` dc_bg. Kotlin: grid span 3→2 only (MediaActivity, DetailMediaActivity). Left out: tab icons + counts (need data at tab creation), Curated/All Local section headers, View all, sort, search, select mode, quality/Dolby badges, size/status line, more-options button, 5 GHz card, Queue/Cast bar, bottom nav, header subtitle/PRO 4K chip.
- 2026-10-23: T7 — Cast Player (`activity_cast.xml`, both photo and video mode): `dc_bg_media_card` stream card + console capsule, round `dc_bg_player_btn`/`dc_bg_player_stop` buttons, gradient play/pause (`_ic_play/pause_media_player__0` recolored, used only here), `dc_seekbar_progress/thumb`; casting dialog → `dc_bg_dialog` (transparent window), playlist sheet → `dc_bg_sheet` + dark rows; Share Cast (`activity_media_action.xml`) → media card + `dc_bg_cta_gradient`. IDs kept; Kotlin only swaps colors/drawables in adapters + dialog window bg. Left out: CC/4K/16:9/Dolby chips, repeat, Wi-Fi/bitrate stats, Cast Target/"Switch TV" pill, volume slider/% (no data), equalizer animation. Prev/next item → tile row; playlist → repeat slot. `bg_item_selected` may now be unused (not deleted).
- 2026-10-23: T9 — Connect Devices: `activity_search_tv` dark (glass scan card, `dc_bg_media_card` connection guide with `dc_bg_step_badge` 1/2/3 + `@string/user_guide` heading, `dc_bg_cta_pill` "Cast Web Browser", connecting overlay → `dc_bg_dialog`); `layout_item_device` → Figma device card (`dc_bg_device_card` r16, `dc_bg_device_icon` well); Wi-Fi off / Samsung check / pairing / PIN / disconnect dialogs → `dc_bg_dialog` + `dc_bg_cta_gradient` / `dc_bg_button_secondary`; Install Channel Roku self-styled (dark bg, `dc_bg_sheet`, in-place restyle of its own `bg_button_all(_off)`, `button_number_add_channel`, `ic_dot_*`). Kotlin: `PairingAlertDialog` transparent window bg only. Left out: radar animation, network name/band + rescan, "Available Screens"/"N Found"/"Tap to pair" header, per-device room/status/protocol/signal/PRO, QR button, guide image card, bottom nav. `button_number_add_channel_on/off` now unused (not deleted).
- 2026-10-23: T10 — Screen Mirroring: `activity_screen_mirroring` dark header (dc_bg_key back), section heading + FAQ moved into the Figma "Setup Guide" pill slot (`dc_bg_chip`, cyan, still `@string/text_faq`), 3 mode cards `dc_bg_mirror_card` (#171c25 r28) with gradient circle icons (`dc_bg_mirror_icon_smart/direct/web`: coral/amber/indigo, `dc_ic_screen_share`, `dc_ic_laptop`) + chevron; smart/direct/web fragments dark + glass instruction card + `dc_bg_cta_mirror` pill CTA (#3131c0→#00f2fe→#3131c0, text #006a70), web status overlay `dc_scrim`/`dc_bg_dialog`; FAQ + `item_faq` glass rows; QR Scan self-styled (dark header, cyan frame, scrim mask). IDs kept, no Kotlin change. Left out: hero TV scene + latency/FPS/Dolby/Wi-Fi chips (no asset/data; old `bg_header_miror` has baked-in white fade), RECOMMENDED/No Wi-Fi/WebRTC badges, device micro-chips, card status lines, Stream Tuning, Spatial Audio, activity-level Start CTA, bottom nav, active-card state. `bg_header_miror`, `bg_button_miror_tv/web`, `bg_button_mirror_direct`, `bg_item_faq`, `ic_mirror_tv/direct/web` may now be unused (not deleted).
- 2026-10-23: T4c — Brand remotes (Samsung/LG/Sony/Android TV/Fire TV): remote-only key drawables restyled in place (`item_btn_cicler` → round `dc_bg_key` oval, `item_btn_cicler_power(_sony)` → red power oval, `item_btn_big/small_type*` → `dc_bg_key`, `bg_vol_type0` rocker + pressed gradients recolored, `bg_on_select_ss`/`bg_default_ss` → chip active/idle, `bg_tab_remote` → glass); brand D-pads use the T4b `bg_control*` family (Kotlin: `bindPressedBackground` drawable IDs only) + OK label; touchpads → `dc_bg_glass_card` with hint (`txt_tab`) + BACK/EXIT labels (were baked into `img_touchpad_ss*` raster; Samsung hint switches with mouse mode via one `setText`; new strings `touchpad_hint_mouse/back/exit`, English only); number pad via `item_btn_cicler`; channels (empty/status/connect CTA, Sony row glass), keyboard + Fire TV connect dialogs (`dc_bg_dialog`, transparent window), Guide FireTV dark. Left out: `bottom_sheet_layout_dot` color-keys sheet (not in scope), key icon tint. `img_touchpad_ss(_mouse)`, `bg_btn_home_ss*`, `ic_bg_btn_home_lg*`, `ic_bg_home_andoridtv*`, `img_remote_fire_tv*`, `img_remote_sony_*` (base still used by `activity_media_action`), `btn_small_cicler_*`, `btn_cicler_power_*`, `btn_type0_*`, `btn_small_type*`, `color_background_remote` may now be unused (not deleted).
- 2026-10-23: T11 — Web Cast (`activity_web_cast`: dc_bg, omnibox `bg_search`, `dc_bg_key` back, DC.Text.Heading section titles, hub tiles `bg_item_web_cast` glass r16 with single-line labels, `dc_surface` browser toolbar with recolored `ic_yt_*`, `dc_scrim` + `bg_data_web_cast` capsule with gradient count badges); history rows `item_content_history`/`item_history` → "Recent Sessions" cards (icon well `dc_bg_device_icon`); History screen dark header; YouTube/Vimeo browser chrome (`dc_surface` toolbar, cyan progress) + `dialog_bsf_youtube` → `dc_bg_sheet`, `item_youtube`, `dialog_no_resource_found` → `dc_bg_dialog`/`dc_bg_cta_gradient`; `available_files_dialog`, `download_result_list`; File Browser self-styled like T10 web mirror; `bottom_sheet_code` → `dc_bg_sheet`, `sample_background` (Pinview, only user) dark so PIN digits are not white-on-white. IDs kept, no Kotlin change. Left out: Twitch/TikTok/IMDb/LAN hub cards layout (kept existing 4x2 site grid), hub chips/subtitles, featured card, Cast Sniffer, Saved Streams tab, transmitting-to bar, mic button, replay button (row keeps remove X), bottom nav. `bg_border_height`, `bg_bottom_sheet_16_main`, `bg_press_screen_mirror` may now be unused (not deleted).
- 2026-10-23: T12 — Splash/Language/Intro/NewIntro: ambient backdrops `dc_bg_ambient_splash` (also `Theme.Splash` window bg), `dc_bg_ambient_top` (Language), `dc_bg_ambient_onboarding` (Intro); splash Lottie assets recolored (text white, TV cyan on `dc_surface`, dots cyan). Language → Figma rows (`dc_bg_language_item` selected gradient + cyan stroke, flag in `dc_bg_icon_button` well, `dc_radio`), round back button, `dc_bg_cta_done` pill, centered title. Intro → glass hero card, DC text, Skip chip moved top-end, cyan dots, `dc_bg_cta_onboarding` arrow pill (`ic_next` now a bare arrow). NewIntro → dc_bg, banner fade (`dc_scrim_fade_bottom`), onboarding-gradient CTA, cyan dots, dark connect dialog/close button, device rows (`bg_border12_*` restyled in place). Kotlin: `root.isSelected` on language rows; IntroActivity no longer forces the light `color_intro` status bar. Left out: Language search box, description, native-name subtitles, ACTIVE/HOT badges, bottom "Confirm Language" CTA; Splash version/protocol footer + tagline; Onboarding "Ultra-Connect" pill, feature badges, gradient headline; DynamicProgressBar colors (set in code). Main splash Lottie does not render at all, same with the original asset (I-022). `bg_splash`, `bg_btn_done` (still used by `activity_language_first2`), `ic_checked_iap` may now be unused (not deleted).
- 2026-10-23: T13 — Settings (`activity_setting`: VIP card `dc_bg_vip_card` + gold CTA `dc_bg_cta_gold` (shine kept), gold check bullets, glass groups via new `DC.Settings.*` styles, accent icon tiles `dc_bg_icon_tile_tint` + existing icons, cyan language value, `ic_vibrate` restyled in place as Figma toggle, version footer); Feedback (dark ambient bg, ring hero with `ic_feedback`, chips → wrapping `Flow`, `bg_item_feedback*`/`bg_button_feedback*` restyled in place, `dc_bg_textarea`); Help (dark header, `help.html` CSS → dark glass Q&A cards); How To (dc_bg, glass step cards, `dc_bg_cta_gradient`). Kotlin: version text from `PackageInfo.versionName` (was hardcoded "14.7.4"), unselected feedback chip color black → `dc_text_body`. Left out: search button, Auto-Connect toggle, PRO VIP chip, row subtitles, Rate 5.0 badge, auto-renew price line (needs billing query), section icons; Help search/topic chips/guide carousel/support card; Feedback History, diagnostics, attachment, email field, section labels. `img_setting`, `bg_rounder_white`, `gradient_button`, `ic_check_premium`, `img_feedback`, `ic_vibrate_on/off`, `ic_version`, `color_setting` may now be unused (not deleted).
- 2026-10-23: T14 — Exit dialog (`dialog_exit_app`: `dc_bg_dialog_opaque` card, red power tile `dc_bg_icon_danger` + `ic_power_v2` #fb7185, stacked `dc_bg_cta_danger` Exit + `dc_bg_button_secondary` Cancel), Rate dialog (`dialog_rate`: padded wrapper so the card has side margins, DC title/body, smaller stars, `dc_bg_cta_gradient` CAPS CTA/Submit, `dc_bg_textarea` feedback), Permission sheet (`dialog_permission`, all 4 types: `dc_bg_sheet` + drag handle, DC text, gradient Allow, muted Deny), Reminder notification accent `setColor(dc_primary)` (only visible where the system tints the small icon). `dc_bg_dialog_opaque` = `dc_bg_dialog` over `dc_surface` (no background blur on Android). IDs kept; no logic change. Left out: Exit "Lossless Cast Engine" status pill and Figma copy (strings kept), Rate sparkle/glow illustration (existing `imv_empty_rate` kept), heart icon, visible "Maybe Later" (`tv_exit` was already `gone`), notification feature checklist + bell hero (existing images kept), custom RemoteViews notification. Empty stars (`img_star_unfill`) are low-contrast on dark. `bg_permission`, `bg_feedback` may now be unused (not deleted).

## Completion Notes

**Summary.** T-001…T-015 done (loop: quality gate passed + reviewer approved each). All 39 screens restyled to
Dark Cinema (18 from Figma frames, the rest self-styled from T1 `dc_*` tokens/components). View IDs kept; Kotlin
touched only for view binding, colors/drawables, grid spans and a few UI-only fixes listed per task above.
Design source moved to Figma: `docs/DESIGN.md`, ADR-0002, `docs/references/design-system-reference-llms.txt`.

**Verification evidence.** Per-task screenshots vs. the Figma PNG and click-handler walkthroughs are in each board
task's submit summary/notes (`node harness/cli.mjs task show T-0xx`). Quality gate passed for every task.

**Known limitations.**
- No real-TV smoke test of the remotes (T-005/T-006 notes); premium state, number pad, keyboard/connect dialogs,
  populated channel lists and Fire TV remote not captured. Fire TV touchpad (`ctTouchpadLG`) has no hint/BACK/EXIT labels.
- Every "Left out" item above is a visible gap vs. the Figma frames (no behaviour behind it).
- New strings are English only (`restore_purchase`, `remote_tab_*`, `touchpad_hint_*`).
- Empty rating stars (`img_star_unfill`) are low-contrast on dark; DynamicProgressBar colors still set in code.
- Splash main Lottie does not render (I-022, pre-existing with the original asset).
- PRO promo video/music removed (product decision, possible conversion impact).

**Follow-up debt.** TD-011 (legacy resources possibly unused, listed per task as "may now be unused").
- 2026-10-23: Main Hub → Stitch "Main Hub Redesigned Menu (Dark Cinema)" (project "Cast to TV Reskin"): ambient bg (`dc_bg_ambient_main`), cyan-edged device card, dark broadcast hero with decorative "Start" pill, Remote/Browser quick tiles, Video/Photo media cards, Material outline icons (`dc_ic_*`). UI only, all existing click targets kept. Left out (no feature behind them): TV status subtitle, version chip, Switch / Remote Console / Live Channels pills, latency/FPS chips, QR Scanner, Music, Cloud Drive, format badges, "All Formats", and on request the whole Integrated Channels row — so YouTube and Vimeo have no entry point from Home anymore (`Feature.YOUTUBE/VIMEO` kept in `MainActivity` for re-adding). PRO badge stays the `anim_premium`/`anim_member` Lottie (state-driven).
- 2026-10-23: PRO Upgrade → Stitch "PRO Upgrade (Dark Cinema)": ambient bg, "Get Premium" chip with pulsing dot, `anim_home` device stage with ping ring, cyan-gradient title + `to_fulfill_your_uninterrupted_experience` subtitle, compact feature cards with outline icons (shared with SubSale), CTA arrow + shimmer sweep, glass close button. Motion lives in `PurchaseActivity.startDecorations()` (cancelled in `onDestroy`). Left out: feature-card subtitles and per-month price line (no strings / no data), Stitch "Extended Subscription Options" plan selection (radio, trial CTA, fixed prices) — the Monthly/Lifetime cards under "More options" are only restyled; they still have no click handler or price (pre-existing gap).
- 2026-10-24: Main Hub, second pass for Stitch parity: device card now has the radar well (+ green dot when connected), Connect/Disconnect pill (state text, card is still the one click target) and two shortcut pills — Remote Control (→ `RemoteActivity`) and Channel (→ `RemoteActivity` on the Channels tab via new `RemoteActivity.EXTRA_OPEN_CHANNELS`). Mirror hero art is now built from views (TV with app bars + phone) instead of `anim_home.json`. YouTube is back as the 3rd quick tile (Remote · Browser · YouTube); Vimeo stays out.
- 2026-10-24: PRO Upgrade hero rebuilt from the Stitch markup with views instead of `anim_home.json`: TV (dark bezel, gradient screen with top light ray, wifi wedge `dc_ic_wifi_signal` over the existing ping, "Connected" label from `text_connected`), cyan base accent, phone (notch, play widget, two mini keys; CAST/SYNC labels left out, no strings), dashed beam `dc_pro_wave` (pre-cut dashes, VectorDrawable has no dash array). Ping parents set `clipChildren=false` so the 2x ring is not clipped square. Left out: "60 FPS" tag.
- 2026-10-24: Remote → Stitch "Smart Remote Redesigned (Dark Cinema)". Shared by all brands: own header `layout_remote_header.xml` (glass back key, centered "Remote Control" + TV chip with dot, cyan cast key; same IDs RemoteActivity used from `item_header`, which other screens keep), cyan active tab (`bg_item_tab`), round glass D-pad with dark cyan-rimmed OK (`bg_control`, `dc_dpad_press_up/ok` spotlight overlays), OK label `#cffafe`. Roku/default fragment only: BACK/HOME/MENU/POWER labels (new `remote_key_back/menu/power`, translated for the 12 locales that have `home`, `tools:ignore="MissingTranslation"` for the rest like the existing strings), mic/keyboard/headphones row, media rail with highlighted play/pause, volume rail with boxed mute. Samsung/LG/Sony/Android TV/Fire TV fragments get the shared parts only; their key grids are unchanged (not verified on device, needs those TVs).
- 2026-10-24: Splash → Stitch "Splash Screen (Dark Cinema)", built from views (replaces `anim_splash.json`, which never rendered — I-022 — and `anim_dot_splash.json`): drifting cyan/purple lights, radar rings (0.85→2.2x, 2.8 s, second ring +0.9 s), glass TV with 16px grid, bouncing waveform bars, gloss and base, tilted phone with pinging cast disc, beam flow, "Cast To TV" title, four loader pills + "Loading..." with ping. Motion in `SplashActivity.startMotion()` (cancelled in `onDestroy`); pills spread over the time left of the unchanged 2 s delay after the first frame. Left out: tagline, "4K HDR Ultra Lossless", "CASTING", "Initializing 60FPS…", engine/protocol footer (no strings / untrue). The Android 12+ system splash (icon on white disc) still shows first on cold start.
- 2026-10-24: Photo Cast / Video Hub / Video Player → Stitch "Photo Cast", "Video Hub", "Video Player & Stream (Dark Cinema)". New shared `layout_media_header.xml` + `BaseActivity.bindMediaHeader()` (glass back, title, TV line with dot / "Tap to connect", cyan cast key; `item_header` screens untouched). Photo: album chips with photo-count badge on the selected one (white label), tiles with bottom scrim + name. Video Hub: tabs with icon + count (`layout_media_tab.xml`), bolder titles. Cast player video mode rebuilt: casting bar (Connected + TV name), stage with seek bar, transport (−15s / previous / play-pause / next / +15s — the app seeks 15 s, not the design 10 s), now-playing card, volume rail, Playlist + Stop (new `cast_stop`, 12 locales). Photo mode of the player unchanged (user chose Video Player over Photo Preview). Left out: bottom nav, selection/slideshow bar, date sections, search, multi-select, curated albums, sort, badges, aspect/audio selectors, subtitles, soundbar level, queue, phone-only, sync card. Verified on emulator; the player screenshot used a temporary session seed (removed).
- 2026-10-24: Screen Mirroring mode picker → Stitch "Screen Mirroring (Dark Cinema)", "Select Connection Mode" section: media header (`bindMediaHeader`), ambient bg, bold heading + cyan "Setup Guide"-style FAQ pill, mode cards with rounded-square gradient tiles, larger titles, arrow bottom-right (cyan on the first). IDs and handlers unchanged; the three sub-pages are unchanged. Left out: hero card (latency / FPS / Dolby / Wi-Fi stats), badges, brand chips, status lines, Stream Tuning, "Start Screen Mirroring" CTA, bottom nav.

- 2026-10-24 Screen Mirroring follow-up (user: buttons differ from Stitch): mode icon tiles are now gradient discs (Stitch `rounded-xl` = 3rem on a 56px tile) with filled glyphs (`dc_ic_connected_tv_fill`, `dc_ic_cast_fill`, `dc_ic_laptop_fill`) and a cyan bolt badge on Smart TV; cards drop the stroke, radius 2rem, first card uses surface-container-high/70 (`dc_bg_mirror_card_active`); FAQ pill is #1b2029 without stroke and uses the outlined `help_center`. Coloured tile glow not done: elevation shadows got clipped by the card.
- 2026-10-24 Buttons vs Stitch (Screen Mirroring + "Device Discovery" scanning screen): header icon buttons are round, no stroke (`dc_bg_round_key`, surface-container/70) in `layout_media_header` (so Photo / Video / Mirroring) and `item_header` (back + help, now `dc_ic_help_center` cyan). Remote keeps its rounded-square keys (its Stitch design uses rounded-2xl). Scanning: "Cast Web Browser" pill gains the `language` globe icon and #e0fdff label; device card loses its stroke (surface-container/60) and the icon well loses its ring. Left out (no feature): Wi-Fi capsule + rescan button, radar sweep, "N Found" chip, protocol/signal tags, "Scan TV Display QR Code" button, bottom nav. Mirror sub-pages (`fragment_mirror_direct/web`) still use the square `dc_bg_key` back.
