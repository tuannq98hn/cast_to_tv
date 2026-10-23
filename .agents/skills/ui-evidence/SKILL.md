---
name: ui-evidence
description: Build, install and run the Android app, then capture screenshots and logs as evidence for UI changes and bug reports.
---
# UI evidence (Android)

```bash
adb devices                                   # need one "device"; else: emulator -list-avds; emulator -avd <name> &
./gradlew :app:installDebug
adb shell am start -n com.apps.dearme.journal.notes/.MainActivity
mkdir -p docs/evidence
adb exec-out screencap -p > docs/evidence/<T-or-I-ID>-<state>.png
adb logcat -c   # before the action; then after:
adb logcat -d --pid=$(adb shell pidof com.apps.dearme.journal.notes) > docs/evidence/<ID>.log
```

Useful states to capture: light + dark (`adb shell cmd uimode night yes|no`), large font
(`adb shell settings put system font_scale 1.3`, reset to 1.0), rotation, empty/error states.
Process death: background the app, `adb shell am kill com.apps.dearme.journal.notes`, reopen.

Sample data (debug builds): seed fake entries — moods, hashtags, PROMPT answer, a Paris entry with a
photo — instead of typing them or using the system photo picker:

```bash
adb shell am start -W -n com.apps.dearme.journal.notes/.debug.SeedSampleDataActivity
```

`connectedDebugAndroidTest` (scripts/test/run_ui_tests.sh) uninstalls the app when it finishes, which
wipes the DB. After running it: `./gradlew :app:installDebug`, then seed again.

Compare with the design: `python3 scripts/agent/figma_compare.py <S1..S12> <shots…> -o docs/evidence/<ID>-<S>-vs-figma.png`
(frames and screen ids in `docs/design/figma/README.md`, rules in `docs/validation/visual-parity-rules.md`).
Measure it: `adb exec-out uiautomator dump /dev/tty > dump.xml`, then
`python3 scripts/agent/measure_ui.py dump.xml shot.png` and put the rows that matter in the task note next to the
spec values from `docs/design/figma/<S>-spec.md` (write that spec from the Figma node first if it doesn't exist).

Reference the files in the task note / bug report. Use fake data only.
No device available → say so explicitly and describe manual steps for the human instead.

For lock-screen captures only, a debug build can skip `FLAG_SECURE` when opted in with an app-private
file. Enable with `adb shell run-as com.apps.dearme.journal.notes touch files/debug_allow_screenshots`
and remove immediately after capture with `adb shell run-as com.apps.dearme.journal.notes rm files/debug_allow_screenshots`.
Never create this file in a release build; release builds always keep `FLAG_SECURE` enabled.
