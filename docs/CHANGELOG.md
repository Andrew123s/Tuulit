# Changelog

## 2.3.0 — 2026-11-25: required accounts, real sign-out, Ask and email fixes

Root causes and design are in `docs/AUTH_AND_SESSION.md`.

### Changed
- **An account is now required.** Signed-out people see the welcome screen with "Create an account" and "I already have an account". Unconfirmed accounts see a verify-email screen. The cloud backup restores before the app opens on a new phone, and returning accounts skip onboarding.
- **Sign out** backs up first, unregisters push, signs out and erases the account's data from the phone (theme and reduced motion are kept). The account and its backup are untouched.
- **Account switching:** local data belongs to one account; a different account signing in erases the previous one's data first. Data from before 2.3.0 is adopted by the first account that signs in.
- **Email verification** uses Firebase's built-in email only. The Brevo verification route was removed.
- Clearer error messages for offline, timeout, expired sign-in, unconfirmed email, busy or unavailable Ask, and unexpected replies.
- Ask skips failed exchanges in the history it sends, so the model answers the latest question.
- Release signing reads `keystore.properties` (git-ignored, see `keystore.properties.example`) before the `TUULIT_KEYSTORE_*` environment variables.
- tuulit.app is authenticated in Brevo (DKIM `brevo1`/`brevo2` and DMARC records confirmed in public DNS).

### Fixed
- Ask failed for every question: the Worker's `AI_API_KEY` secret held a single Ctrl+V character. Answers were also cut off by Gemini's thinking tokens.
- Brevo emails failed for the same reason (`BREVO_API_KEY`).

### Security
- Firestore rules and the Worker (Ask, media, Connect) require a confirmed email.
- The Worker refuses malformed secrets with a `secret_invalid` log, and logs Brevo's error reason with addresses removed.

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
