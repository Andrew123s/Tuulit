# Tuulit development plan

The approved design is the Tuulit Design System artifact. The screen specification and component guidelines there are the source of truth for the UI.

## Phase 1 — Code audit (2026-11-22)

### What exists

- A native Android app built with Kotlin, Jetpack Compose and Material 3, in one activity. There is one 525-line `TuulitViewModel` that every screen uses.
- Data is stored locally only: Room (8 tables) plus DataStore. There is no backend, no account system and no sync.
- There are 9 screen files (about 6,600 lines). They follow the old Gemini design, not the approved one.
- Build: AGP 9.1.1, Gradle 9.3.1, Kotlin 2.4.21, compileSdk 36.1, minSdk 24. There was no Gradle wrapper and no `local.properties`. The release signing block expected AI Studio keystores that don't exist on this machine.

### Keep

- **`CycleCalculator`:** a sound weighted-average prediction with variability ranges, irregular handling, fertile window, Mood Map and pain-pattern detection. It has 6 unit tests.
- **The domain models** (`CycleModels.kt`): flow, symptoms with severity, phases, Banana Mode personas.
- **The Room schema concept:** cycles, daily logs, pain logs, survival kit, checklist, achievements, contacts, game scores.
- **`MedicalSafetyGuard` keyword triage:** moves server-side as well, and stays on the client as a first line.
- **Game logic ideas:** Banana Catch, memory pairs, Calm Tap, Stress Smash, and the quiz content (real, accurate copy).

### Placeholders, fakes and broken pieces

| # | Finding | Where |
|---|---|---|
| 1 | **The AI returns canned answers.** `SampleTuulitAiProvider` picks pre-written strings, and the UI says "Preview Mode — Firebase Required". | `domain/AiProvider.kt`, `AiScreen.kt` |
| 2 | **Connect is fake.** It generates an invite code locally, but nothing is ever sent or accepted. "Local sharing mode" has no counterpart. | `ConnectAndProfileScreen.kt` |
| 3 | **Notifications are simulated.** The settings toggles are saved, but no notification is ever scheduled or posted, and no channel exists. | `UserPreferencesRepository`, no worker |
| 4 | **There is no authentication.** No registration, login, password reset or email verification. | — |
| 5 | **The PIN is stored in plain text** in DataStore. | `UserPreferencesRepository.setPrivateModePin` |
| 6 | **The health database is unencrypted, and `allowBackup="true"`** lets cycle data leave the device in system backups. | `AndroidManifest.xml`, `TuulitDatabase` |
| 7 | **Achievements are seeded** but never shown anywhere meaningful. | `TuulitRepository` |
| 8 | **The design does not match.** All screens use the old card-heavy layout, the tabs are Home/Calendar/Care/Play/Profile, and the fonts are Regular-only subsets mapped to 4 weights. | `ui/` |
| 9 | **Games:** 4 exist (Banana Catch, Mood Match, Calm Tap, Stress Smash). Brain Break, Word Puzzle, Memory Flip (as distinct from Mood Match) and Focus Game do not exist. | `PlayScreen.kt` |
| 10 | **Voice journaling, symptom export UI and the partner view** do not exist. | — |
| 11 | **Dead dependencies:** Retrofit, Moshi, CameraX, Coil, Accompanist, Play Services Location, Credentials/GoogleId are declared but unused. The `secrets` plugin injects a Gemini key from `.env`. | `libs.versions.toml`, `app/build.gradle.kts` |
| 12 | **Placeholder tests:** `ExampleUnitTest`, `ExampleRobolectricTest`, `ExampleInstrumentedTest`. | `app/src/test`, `androidTest` |
| 13 | **Package name and app ID** are `com.example` / `com.aistudio.tuulit.kvnqmr`. | `app/build.gradle.kts` |
| 14 | **The launcher icon** is a 390 KB JPEG used as the adaptive foreground. | `res/drawable` |

## Architecture decisions (summary; details in ARCHITECTURE.md)

1. **Local-first.** Room (SQLCipher-encrypted) is the device's source of truth, so the app works fully offline and without an account.
2. **An account is required only for features that need a server:** backup/sync, Ask (AI), Connect and push notifications. Onboarding offers it and lets the user skip.
3. **Firebase Spark only:** Authentication (email and password), Cloud Firestore, Cloud Messaging and App Check. Cloud Functions are *not* used, because they need the Blaze plan.
4. **One Cloudflare Worker** (`backend/worker`) holds every secret and does all server work:
   - AI proxy (provider is configurable)
   - Brevo transactional email
   - Cloudflare R2 signed URLs
   - FCM sends for Connect
   - Account deletion clean-up

   It verifies Firebase ID tokens on every request.
5. **Cloudflare R2** stores voice-journal audio only, and only when the user chooses to keep the recording. The bucket is private, uploads and downloads use short-lived signed URLs, and structured data stays in Firestore.
6. **Local reminders** (period, check-in) use WorkManager on the device, so no server or FCM is needed. FCM is used for partner and SOS messages, which come from another person.

## Roadmap and status

Live status is kept in `IMPLEMENTATION_STATUS.md`.

| Phase | Scope |
|---|---|
| 1 Audit | This document |
| 2 Foundation | Gradle wrapper, signing, dependency clean-up, package structure, design system in Compose (tokens, fonts, components, mascot), encrypted DB, PIN hashing, backup off |
| 3 Backend | Firebase init with graceful "not configured" state, Auth flows, Firestore sync, security rules, Worker (AI, Brevo, R2, FCM, deletion) |
| 4 Core tracking | Onboarding, Today, Cycle (Month and History), Log composer, Insights, Pain Journal |
| 5 Care & Play | Comfort, Sessions, Library, 8 games, 4 quizzes, achievements |
| 6 AI | Ask home, chat, Explain My Body, Myth or fact, Appointment Prep, consent and safety |
| 7 Connect | Invites, acceptance, permissions, revoke, SOS, partner view |
| 8 Advanced | Voice journaling, export, Today Is Not the Day, Banana Mode, Couple quiz, notifications |
| 9 Testing | Unit, Room, repository, ViewModel, Compose UI, rules tests |
| 10 Polish | Compare against the design system, accessibility pass |

## What the owner must provide (blocked without it)

| Item | Needed for | Where it goes |
|---|---|---|
| Firebase project + `app/google-services.json` | Auth, Firestore, FCM, App Check | `FIREBASE_SETUP.md` |
| Firebase service account JSON (as a Worker secret) | FCM sends and admin deletion from the Worker | `FIREBASE_SETUP.md`, `backend/worker` |
| Cloudflare account, R2 bucket, R2 API token | Voice-note audio | `CLOUDFLARE_R2_SETUP.md` |
| Brevo account, API key, verified sender | Emails | `BREVO_SETUP.md` |
| AI provider API key (Anthropic by default; configurable) | Ask | `backend/worker/README.md` |
| Deployed Worker URL | The app's `TUULIT_API_BASE_URL` | `local.properties` / CI env |
