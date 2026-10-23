# Test Matrix

Use this file to map important behavior to tests. "—" = none yet; add a row's test when you touch it.

| Feature/Area | Unit | UI/Instrumented | Manual | Notes |
|---|---|---|---|---|
| Onboarding order (`Onboarding`) | — | — | Fresh install: Splash → Language → Main | Pure branching; good first unit test target with fake prefs |
| Premium entitlement (`Premium`) | — | — | Play test track purchase + restore | Legacy product IDs must still grant premium |
| Device discovery + connect | — | — | Same Wi-Fi as a real TV per brand | connectsdk; emulator cannot see LAN TVs |
| Cast photo / video / audio | — | — | Cast from gallery, seek a video | `FileServer` Range handling is unit-testable |
| Web cast (`WebReceiver`) | — | — | Open `http://<phone>:8686` in TV browser | |
| Screen mirroring to TV | — | — | Start/stop from app and notification | MediaProjection, foreground service |
| Web mirroring (QR) | — | — | Scan bcastmirror.com QR | Needs internet |
| Remote: Samsung / LG / Roku / Sony / Android TV / Fire TV | — | — | Each key class, pairing prompts | Message encoding (JSON/IRCC/ECP URLs) is unit-testable |
| Browser history (`BrowserHistory`) | — | — | Add/remove/clear survives restart | JSON in prefs |
| Share-to-app (`MediaActionActivity`) | — | — | Share image/video from gallery | |

## Test Naming Rules

- Test names should describe behavior, not implementation.
- Tests should cover success, failure, empty, and edge states where relevant.
- Add regression tests for important bugs.
