# Implementation status

Updated 2026-11-23.

Legend:
- ✅ **Completed and tested**: automated tests and/or verified on the emulator.
- 🟡 **Implemented, not fully tested**: code complete, but it needs a configured backend or more hands-on testing.
- 🔵 **In progress**
- ⛔ **Blocked**: waiting on credentials only the owner can create.
- ⚪ **Not started**

The emulator used was Pixel-class, API 35, software-rendered. On-device checks: onboarding, Today, log composer, Cycle month, Care Comfort, Play hub, Stress Smash through to its result, Ask (not-configured state) and You.

## UI redesign (2.1.0)

See [UI_REDESIGN.md](UI_REDESIGN.md).

| Item | Status | Notes |
|---|---|---|
| Light-first warm palette; default theme is now Light | ✅ | Verified on the emulator. Warm dark is still available in Appearance. |
| Illustration system (blobs, cycle ring, mood blobs, spot art, game art) | ✅ | Canvas only, no image assets. |
| Home, Cycle, Log, Care, Play, Ask, navigation | ✅ | Each one screenshotted on the emulator after the change. |
| You, Connect, Insights, Pain, Auth PIN, quizzes, sheets, Welcome | ✅ | You and Connect were checked on the emulator. The others are colour-only changes covered by the build. |
| Unit and Compose UI tests | ✅ | 44 tests, 0 failures. |

## Foundation

| Item | Status | Notes |
|---|---|---|
| Gradle wrapper, signing from env vars, dependency clean-up | ✅ | Debug and release (R8) build. The release build runs on the emulator. |
| Package `app.tuulit`, modular package structure | ✅ | |
| Design system in Compose: tokens, variable fonts, icons, mascot, components | ✅ | Compose UI tests. Matched against the design screens on the emulator. |
| Encrypted Room DB (SQLCipher + Keystore), backups disabled | ✅ | Verified on the emulator. Raw-key mode avoids slow KDF on every open. |
| PIN hashing (PBKDF2), auto re-lock, FLAG_SECURE when locked | ✅ | Unit tested. The lock screen was not exercised on the emulator. |

## Backend

| Item | Status | Notes |
|---|---|---|
| Firestore schema and security rules | ✅ | 7 emulator tests |
| Cloudflare Worker: auth, AI, Brevo, R2, FCM, deletion | 🟡 | 9 unit tests, typecheck and bundle pass. **Not deployed:** needs Cloudflare account, secrets and Firebase project. |
| Firebase project and `google-services.json` | ⛔ | Owner action: `FIREBASE_SETUP.md` |
| Brevo account and verified sender | ⛔ | Owner action: `BREVO_SETUP.md` |
| AI provider key | ⛔ | Owner action: `CLOUDFLARE_R2_SETUP.md` §4 |

## Features

| Feature | Status | Notes |
|---|---|---|
| Onboarding: welcome, last period, cycle basics, privacy and reminders | ✅ | Run end to end on the emulator. Bug found and fixed: the last period wasn't saved. |
| Registration, login, logout, password reset, email verification, session persistence | 🟡 | Firebase Auth code complete, with friendly errors and validation (unit tested). Needs a Firebase project to run. |
| Today: hero countdown, ribbon, mood check-in, suggestion, I Need Something, SOS entry | ✅ | Emulator plus hero and suggestion unit tests |
| Log composer: flow, mood, moods, symptoms with severity, energy, sleep, water, notes, medication, day navigation, clear day | ✅ | Emulator plus repository tests |
| Cycle: month calendar, day summary, history, edit and delete periods | ✅ | Emulator (month). Edit and delete covered by repository tests. |
| Predictions with uncertainty ranges and fertile-window estimate (with "not contraception" copy) | ✅ | CycleCalculator tests |
| Insights, Cycle Mood Map, symptom frequency, CSV export | 🟡 | Logic tested. The screen needs ≥2 cycles of data to check visually. |
| Pain Journal with pattern alert and red-flag notice | 🟡 | Repository tested. Screen not exercised on the emulator. |
| Care: Comfort hero by log, 6 guided sessions with breathing animation, red flags | 🟡 | Comfort verified on the emulator. Session player not run end to end. |
| Comfort Library and Survival Kit (add, check, delete, helped count) | 🟡 | Repository tested |
| Play: 8 games (Banana Catch, Mood Match, Memory Flip, Brain Break, Word Puzzle, Calm Tap, Focus Game, Stress Smash) with scoring, restart and result | 🟡 | Stress Smash verified with result and personal best. The others compile and share the same result flow; they need play-testing. |
| Quizzes (4, including reflective and couple) | 🟡 | Content integrity tested. Not run on the emulator. |
| Achievements | ✅ | Unlock-once logic tested |
| Ask: chat, Explain My Body, Myth or fact, Appointment Prep (PDF), consent, safety notice | 🟡 | The "not connected" state was verified on the emulator. Live answers need the deployed Worker and an AI key. |
| Connect: invite, accept, permissions, revoke, SOS push, partner view, replies | 🟡 | Rules tested in the emulator. App flow needs Firebase plus the Worker. |
| SOS without an account (share-sheet fallback) | 🟡 | Implemented. Not run on the emulator. |
| Banana Mode, Today Is Not the Day | 🟡 | Implemented. Not run on the emulator. |
| Voice journaling (dictate, record, play, R2 backup) | 🟡 | Local record and dictate implemented. Backup needs the Worker. Not run on the emulator. |
| Local reminders (period heads-up, check-in) with quiet hours and discreet text | 🟡 | Scheduling rules unit tested. Delivery not observed on the emulator. |
| Push notifications (FCM) for Connect | 🟡 | Needs Firebase plus the Worker |
| Account deletion and device wipe | 🟡 | Implemented end to end. Needs Firebase to test the account path. |
| Dark theme ("Warm dark"), reduced motion | 🟡 | Implemented. Not checked on the emulator. |

## Known issues and next steps

1. Startup on the software-rendered emulator takes about 9 s (release) or about 17 s (debug), mostly class loading and the first Compose frame. Measure on real hardware and add a Baseline Profile if needed.
2. Once, right after launch, the Ask tab was highlighted while Today content was still showing. It didn't reproduce on retry. Watch for it during device testing.
3. Play-test the timing-based games (Banana Catch difficulty curve, Calm Tap window, Focus speed).
4. Add instrumented end-to-end tests once a Firebase test project exists (auth, sync restore, Connect between two accounts).
5. Tablet layout uses the phone layout centred at 600 dp. A navigation rail for ≥600 dp is still to do.
