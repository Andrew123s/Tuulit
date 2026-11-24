# iOS testing

| Suite | Where | What it covers |
|---|---|---|
| TuulitCore unit tests (`ios/Packages/TuulitCore/Tests`) | `swift test`, on Linux and macOS in CI | Cycle predictions (ported Android tests with the same numbers), Insights, pain pattern, safety guard, validation, PBKDF2 vectors, reminder rules, epoch-day conversion including time zones, tracking repository (ported), CSV export, Care and Play repository, sync merge with Android-shaped documents, settings sync, Connect status fields |
| App tests (`ios/TuulitTests`) | Xcode, iPhone simulators | Android route strings, navigation model, logging to snapshot, onboarding, wiping the phone, reminder planning (quiet hours, discretion, logged days), design tokens against Android values |
| UI tests (`ios/TuulitUITests`) | Xcode, iPhone SE (3rd gen), iPhone 16, iPhone 16 Pro Max | Onboarding, all four tabs, logging a period, the Log sheet, playing a game through to its result |
| Worker (`backend/worker/test`) | `npm test` | Includes the iOS push message (discreet APNs alert; Android data-only message unchanged) |
| Android (`app/src/test`) | `./gradlew :app:testDebugUnitTest` | Includes settings sync and the fixed starter-kit IDs |

CI runs on every push that touches `ios/` (see `.github/workflows/ios.yml`). The UI tests launch with `-uitest`, which uses an in-memory store and keeps Firebase off, so they need no secrets and never touch real data.

## Manual checks still needed on a real iPhone

These need a real device, or setup that CI can't do:
- Push notifications (APNs key required)
- App Attest
- Face-down privacy cover in the app switcher
- Dictation and recording permissions
- An Android ↔ iPhone sync round trip on the same account (steps in `IOS_FIREBASE_SETUP.md`)
