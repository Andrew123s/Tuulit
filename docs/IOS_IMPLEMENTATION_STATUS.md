# iOS implementation status

Updated 2026-11-23.

> **Nothing in `ios/` has been compiled or run yet.** This PC has no Mac, no Xcode and no Swift toolchain. Every iOS item below is "Implemented, not tested" until `.github/workflows/ios.yml` (or a Mac) builds it and runs the tests.

| Area | Status | Notes |
|---|---|---|
| Android audit, parity list, architecture, sync design | Completed | `IOS_FEATURE_PARITY.md`, `IOS_ARCHITECTURE.md`, `IOS_DATABASE_AND_SYNC.md` |
| TuulitCore (cycle maths, safety guard, content, validation, PIN hashing, store, repositories, sync merge) | Implemented, not tested | Android tests ported, plus sync and epoch-day tests |
| Design system (tokens, fonts, SVG icons, mascot, illustrations, components) | Implemented, not tested | |
| Screens (onboarding, auth, Today, Log with voice, Cycle, history, Insights, Pain Journal, Care, sessions, library, Play with 8 games and quizzes, Ask, appointment PDF, Connect, You and settings, lock, sheets) | Implemented, not tested | |
| Services (Firebase Auth, Firestore sync, Connect, AI and media through the Worker, reminders, push) | Implemented, not tested | Needs `GoogleService-Info.plist` and an APNs key |
| Worker: APNs alert for `platform: ios` | Completed and tested | 10/10 Worker tests pass; typecheck clean |
| CI workflow (Linux core tests; macOS build and tests on 3 iPhone sizes) | Written, never run | |
| Android changes for full cross-platform sync | Blocked | Awaiting your approval (see sync doc) |
| `IOS_FIREBASE_SETUP.md`, `IOS_TESTING.md`, `IOS_CHANGELOG.md` | Not started | |
