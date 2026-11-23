# iOS implementation plan

| Phase | Scope | Output |
|---|---|---|
| 1. Android audit | Features, architecture, backend, schema, design system | `IOS_FEATURE_PARITY.md`, `IOS_DATABASE_AND_SYNC.md` |
| 2. Architecture | XcodeGen project, TuulitCore package, app container, navigation, services | `IOS_ARCHITECTURE.md`, `ios/project.yml` |
| 3. Design system | Tokens, fonts, SVG icons, mascot, illustrations, components | `ios/Tuulit/Design` |
| 4. Core app | Auth, onboarding, Today, Cycle, calendar, log composer, You | `ios/Tuulit/Features` |
| 5. Care and Play | Sessions, library, 8 games, 4 quizzes, achievements | `Features/Care`, `Features/Play` |
| 6. AI and Connect | Ask, appointment prep, invites, permissions, partner view, SOS | `Features/Ask`, `Features/Connect` |
| 7. Advanced | Pain Journal, Insights and Mood Map, voice journal, reminders, app lock, Banana Mode, Not the Day, export, wipe, delete | across features |
| 8. Synchronisation | Android ↔ iOS round trips against one Firebase project | needs your Firebase keys and both devices |
| 9. Testing | TuulitCore unit tests, app tests, UI tests on iPhone SE, 15 and 16 Pro Max simulators | `.github/workflows/ios.yml` |
| 10. Polish | Accessibility pass, Dynamic Type, VoiceOver, haptics, Reduce Motion | |

## What is needed from you

1. **A Mac with Xcode 16 or later**, *or* GitHub Actions access. With access, the macOS workflow in `.github/workflows/ios.yml` builds the app and runs every test. The repository is private, so I need `gh auth login` on this PC to read the results.
2. **`GoogleService-Info.plist`** for an iOS app registered in the *same* Firebase project, with bundle ID `app.tuulit`.
3. **An APNs auth key** (.p8) uploaded in Firebase → Cloud Messaging, so Connect pushes reach iPhones.
4. **Approval for the Android changes** listed in `IOS_DATABASE_AND_SYNC.md`: pull on foreground, settings sync and fixed kit seed IDs.
5. **An Apple Developer team ID**, needed for device builds, App Attest and TestFlight.
