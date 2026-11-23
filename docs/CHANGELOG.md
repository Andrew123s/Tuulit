# Changelog

## 2.1.0 — 2026-11-23: warm, light-first UI redesign

This release responds to tester feedback that 2.0 felt basic and depressing. Details are in `docs/UI_REDESIGN.md`.

### Changed
- **Theme:**
  - the default theme is now Light (it was System, which put dark-mode phones on near-black)
  - new palette: ivory, banana, coral, peach, dusty rose, muted lilac, cocoa
  - selected states use soft colour instead of solid charcoal
- **Home:**
  - greeting
  - phase-tinted hero with the cycle ring
  - quick actions
  - mood blobs
  - recommendation and "What would help" cards
- **Cycle:**
  - summary card
  - round calendar days
  - pill legend
- **Log:** colour-coded flow, mood, symptom tiles and a banana Done button.
- **Care:**
  - featured session
  - category filter
  - coloured carousel
  - library preview
- **Play:**
  - featured game
  - today's pick
  - progress
  - illustrated game grid
  - coloured quizzes
  - tinted game headers
  - redrawn Banana Catch
- **Ask:**
  - hero with the mascot
  - six shortcuts
  - mascot avatar
  - formatted bubbles
  - typing dots
- **Navigation:** floating pill tab bar and the banana Log button.
- **You and settings:**
  - profile card
  - grouped panel
  - coloured icon tiles on every list row
- Insights, pain scale, PIN, Connect, quizzes and sheets have been recoloured.

### Added
- `ui/designsystem/Illustrations.kt`: blobs, `CycleRing`, `MoodBlob`, spot illustrations and per-game art.
- Mascot blush cheeks.


## 2.0.0 — 2026-11-22: full implementation of the approved design

### Added
- **Approved Tuulit design system in Compose:**
  - colour tokens (light and Warm dark)
  - Fraunces and Plus Jakarta Sans variable fonts
  - an icon set generated from the design SVGs
  - the vector banana mascot with seven moods
  - components (buttons, chips, segmented control, fields, toggles, rows, notices, panels, tiles, sheets, toasts, tab bar with a raised Log button)
  - the cycle ribbon and calendar
- **New navigation:** Today · Cycle · Log · Care · Ask, plus You (profile and settings) from the avatar.
- **Screens:**
  - 3-step onboarding
  - Today
  - log composer
  - Cycle (month and history, with edit and delete)
  - Insights and Mood Map
  - Pain Journal
  - Care (Comfort, Play, Library) and guided sessions
  - 8 games and 4 quizzes
  - Ask (chat, Explain, Myth, Appointment Prep)
  - Connect (invite, accept, permissions, partner view)
  - You (cycle settings, Banana Mode, privacy, notifications, appearance, account, help)
  - App lock
  - empty, loading and error states
- **Firebase (Spark) integration:**
  - email and password auth
  - Firestore backup and sync with tombstones and last-write-wins merge
  - Connect via rules-protected documents
  - FCM token registration
  - App Check (Play Integrity, debug provider in debug builds)
- **Cloudflare Worker (`backend/worker`):**
  - Firebase ID token and App Check verification
  - configurable AI proxy (Anthropic or OpenAI-compatible) with a server-side safety check
  - Brevo welcome, verification and deletion emails
  - R2 voice-note storage through HMAC-signed URLs
  - FCM sends for Connect
  - account deletion
  - per-user rate limits
- **Firestore security rules** (`firebase/firestore.rules`) and an emulator test suite.
- **Local reminders with WorkManager** (period heads-up, evening check-in), with quiet hours and discreet lock-screen text.
- **Voice journaling** (dictate to note, record, play, optional private backup).
- **Achievements earned by actions.** Personal-best scores. CSV export. PDF appointment summary.
- **Tests:** 44 Android (unit, Room, MockWebServer, Compose UI), 9 Worker, 7 Firestore rules.
- **Docs:** every file listed in `DEVELOPMENT_PLAN.md`.

### Changed
- Package and application ID changed from `com.example` / `com.aistudio.tuulit.kvnqmr` to `app.tuulit`. minSdk changed from 24 to 26, because the app uses `java.time` and font variation settings.
- The database is now SQLCipher-encrypted. Records use UUIDs and carry `updatedAt` and `deleted` for sync.
- `CycleCalculator` and the domain models were kept. IDs became strings for sync, and a mood score was added.
- The medical safety guard was rewritten with word-boundary rules and urgency levels. It no longer flags phrases like "Biology 101".
- Release signing now reads `TUULIT_KEYSTORE_*` environment variables. Release builds use R8.

### Removed (and why)
- **Old Gemini UI** (`com/example/ui/**`, 9 screens and `TuulitViewModel`): replaced by the approved design. A copy was kept outside the repo during the work.
- **`SampleTuulitAiProvider`:** returned canned answers presented as AI. Replaced by the real Worker integration.
- **Fake Connect** (local invite codes that nothing could accept): replaced by real Firestore-backed connections.
- **`metadata.json` and `.env.example`:** AI Studio scaffolding that expected a Gemini key in the APK. Secrets now live only in the Worker.
- **`secrets` Gradle plugin; unused Retrofit, Moshi, CameraX, Coil, Accompanist, Play Services Location, Credentials and GoogleId dependencies.**
- **Regular-only subset fonts and JetBrains Mono:** replaced by full variable fonts.
- **390 KB JPEG launcher icon:** replaced by a vector adaptive icon with a monochrome layer.
- **`Example*Test` placeholder tests.**
- **The original AI Studio prompt:** moved to `docs/archive/original-ai-studio-brief.md`, not deleted.
