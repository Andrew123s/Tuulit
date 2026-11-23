# iOS architecture

Tuulit for iPhone is a native SwiftUI app in `ios/`. Its rules:

- It shares the Android app's backend: Firebase Spark and the Cloudflare Worker.
- It ports the Android domain logic line for line, so both platforms calculate the same predictions.
- It has no web views and uses no cross-platform framework.

## Layout

```
ios/
  project.yml                 XcodeGen spec (the .xcodeproj is generated, not committed)
  Config/Tuulit.xcconfig      non-secret build settings; optional Local.xcconfig (gitignored)
  Packages/TuulitCore/        pure Swift package: no UIKit, SwiftUI or Firebase
    Sources/TuulitCore/       models, CycleCalculator, safety guard, content, validation,
                              PIN hashing, local store, repositories, sync merge rules
    Tests/TuulitCoreTests/    ports of the Android unit tests, plus sync and epoch-day tests
  Tuulit/                     the app target
    App/                      entry point, AppDelegate (push), AppModel (dependency container), root view
    Design/                   tokens, fonts, SVG path parser, icons, mascot, illustrations, components
    Services/                 Firebase, Auth, API client, Sync, Connect, AI, Media, Account, Reminders
    Features/                 Today, Cycle, Log, Care, Play, Ask, Connect, You, Auth, Onboarding, Shell
    Resources/                fonts (copied from Android), asset catalog, Info.plist, privacy manifest
  TuulitNotificationService/  extension: rewrites Connect pushes when discreet mode is off
  TuulitTests/                app-level tests (view models with the in-memory store)
  TuulitUITests/              launch, onboarding and tab navigation on simulators
```

## Why a pure-Swift core package

The domain and data rules are the part that must match Android exactly. These include:

- cycle maths
- flow-to-cycle reconciliation
- tombstones
- last-write-wins merging
- the CSV export

Keeping them in a package with no Apple UI or Firebase imports means three things:

- `swift test` runs them on any Swift toolchain (Linux CI, macOS, or Swift for Windows).
- The Android test cases are ported one for one, with the same inputs and the same expected numbers.
- The app target stays thin: SwiftUI views, plus services that move data between `TuulitCore` and Firebase.

## Patterns

- **MVVM with Observation.** `AppModel` is an `@Observable` container, injected through the SwiftUI environment. Screens read from `CycleState`, which recomputes on every store change and at midnight, the same way Android's `CycleState` does. Screens call repositories directly, or through small `@Observable` view models where a screen keeps working state (log composer, Ask chat).
- **Swift Concurrency.** Services are `async`. `@MainActor` is used for UI state, and the store serialises writes on its own actor.
- **Combine** is used only where Firebase hands out listeners. Connect's snapshot listeners are wrapped as `AsyncStream`.

## Local persistence

**Decision:** an encrypted, versioned, Codable file store (`FileStore`), not SwiftData or Core Data.

- **Size.** The data set is small: years of daily logs come to a few thousand rows. The whole database is held in memory and written atomically on every change, so a crash can't half-write it.
- **Encryption at rest.** The file is written with `FileProtectionType.complete`. iOS encrypts it with a key tied to the passcode, and it can't be read while the phone is locked. This is the iOS equivalent of Android's SQLCipher database with its key in the Android Keystore.
- **Excluded from iCloud and iTunes backups** (`isExcludedFromBackup`), matching Android's `allowBackup=false`.
- **Portable.** It runs in TuulitCore tests on Linux, which SwiftData can't do.
- **Schema.** It carries a `schemaVersion`, and migrations are plain Swift functions.

Settings go in `UserDefaults` through a `SettingsStore` protocol; the tests use an in-memory version. The PIN hash is stored there as a PBKDF2 hash, the same as on Android, and never as the PIN itself. Firebase keeps the auth session in the Keychain itself.

## Secrets

The iPhone app never receives a secret.

| Item | Where it lives |
|---|---|
| Firebase client config | `GoogleService-Info.plist` (gitignored; see IOS_FIREBASE_SETUP.md). This is a public client identifier, not a secret, but it is kept out of git to match `google-services.json` |
| Worker URL | `TUULIT_API_BASE_URL` in `Config/Local.xcconfig` (gitignored), passed to Info.plist |
| AI, Brevo, R2 and service-account keys | Only in the Worker (Wrangler secrets), exactly as for Android |
| App Check | App Attest in release builds, the debug provider in debug builds |

## Navigation

- **Tabs.** A custom floating pill tab bar, the same design as Android, with the 66 pt banana Log button in the middle. It sits in a `ZStack` over a `TabView`-equivalent switch, so each tab keeps its own `NavigationStack`.
- **Routes.** These use the Android route strings (`session/{id}`, `game/{id}`, `ask/chat/{tool}?topic=`) through a `Route` enum, so suggestion routes from the shared content resolve the same way.
- **Sheets.** The log composer, I Need Something, SOS and Banana Mode open as native sheets with detents. On iOS this replaces Android's bottom sheets.
- **App lock.** A full-screen cover. It relocks after 60 s in the background, as on Android. The privacy snapshot cover hides the screen in the app switcher.

## Design system

- **Colours.** The tokens are a one-to-one copy of Android `Theme.kt`, both light and Warm dark. The theme modes are System, Light and Warm dark, with Light as the default.
- **Fonts.** The Fraunces and Plus Jakarta Sans variable fonts are bundled from `app/src/main/res/font`. Text styles scale with Dynamic Type through `relativeTo:`.
- **Icons and mascot.** These are drawn from the same SVG path strings as Android, through an SVG path parser in `Design/SVGPath.swift`, so they match exactly.
- **Illustrations.** Blobs, the cycle ring, mood blobs, spot art and game art are ported shape for shape.
- **iOS adaptations:**
  - native sheets with grabbers
  - swipe-back
  - haptics (`sensoryFeedback`) on logging, game hits and SOS
  - safe areas and the Dynamic Island
  - the keyboard avoidance built into SwiftUI
  - Reduce Motion read from the system *or* the in-app setting
