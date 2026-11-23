# iOS feature parity

This list comes from the Android source in `app/src/main/java/app/tuulit` at commit `06b5a33`, not from the docs. Android is the reference.

## Android audit

Each feature is classified as one of:

- **Full**: works on Android.
- **Partial**: on Android, but it depends on a backend that isn't configured yet, or it is limited.
- **Not on Android**: not implemented.
- **Planned**: future work.

### Platform columns

The iOS column uses the status labels from `IOS_IMPLEMENTATION_STATUS.md`. There, "Implemented" means the code is written but has **not been compiled or run**, because there is no Mac in this environment. The **Code** column says where the iOS code lives.

### Accounts and onboarding

| Feature | Android | iOS | Code |
|---|---|---|---|
| Three-step onboarding: welcome, last period, cycle basics, privacy and reminders | Full | Implemented | `Features/Onboarding` |
| Register, sign in and sign out with email and password (Firebase Auth) | Full (needs Firebase config) | Implemented | `Services/AuthService`, `Features/Auth` |
| Password reset (Firebase email) | Full | Implemented | `AuthService.sendPasswordReset` |
| Email verification: Brevo through the Worker, with Firebase's email as the fallback; polls every 15 s | Full | Implemented | `AuthService.sendVerification`, `VerifyEmailNotice` |
| Welcome email through the Worker and Brevo | Full (needs the Worker) | Implemented | same Worker endpoint |
| Session persistence | Full (Firebase) | Implemented (Firebase keychain) | |
| Profile: display name and avatar initials | Full | Implemented | |
| Account deletion through the Worker, with a client-side fallback | Full | Implemented | `AccountService` |

### Tracking

| Feature | Android | iOS | Code |
|---|---|---|---|
| Today: greeting, phase hero, cycle ring, countdown, "My period started" with undo, quick actions, mood check-in, suggestion with "Not today", I Need Something card, account prompt, SOS link | Full | Implemented | `Features/Today` |
| Cycle prediction: weighted average, variability, irregular ranges, fertile window, phases | Full | Implemented and tested in TuulitCore | `CycleCalculator.swift` |
| Calendar: month grid, legend, selected-day detail, long-press to log | Full | Implemented | `Features/Cycle` |
| Period history with edit and delete, including validation | Full | Implemented and tested | `HistoryView`, `TrackingRepository.editCycle` |
| Log composer: flow, mood score, mood chips, custom moods, symptoms with 3 severities, custom symptoms, energy, sleep, water, medication, movement, private note, "period ended", "clear day", day navigation | Full | Implemented | `Features/Log` |
| Flow-to-cycle reconciliation (extend, move start, tombstone reuse, removal) | Full | Implemented and tested | `TrackingRepository.swift` |
| Voice journal: dictate, record, play, delete, private R2 backup | Full | Implemented: Speech framework, AVAudioRecorder, R2 through the Worker | `Features/Log/VoiceJournal` |
| Pain Journal: 1–10 scale, location, impact, type, hours, relief, notes, red-flag notice, history, delete with undo | Full | Implemented | `Features/Cycle/PainJournal` |
| Insights: cycle bars, stats, Cycle Mood Map, symptom frequency, observed patterns | Full | Implemented | `Features/Cycle/Insights` |
| CSV export with formula neutralising | Full | Implemented and tested | `TrackingRepository.exportCSV`, `ShareLink` |

### Care and Play

| Feature | Android | iOS | Code |
|---|---|---|---|
| Care Comfort: featured session, categories, carousel, library preview, red flags | Full | Implemented | `Features/Care` |
| Guided sessions: breathing circle, step timer, pause, next, finish, library credit, achievements | Full | Implemented | `SessionView` |
| Comfort Library, Period Survival Kit and sleep routine | Full | Implemented | `LibraryView` |
| Play hub: featured game, daily pick, progress, game grid, quizzes, moments | Full | Implemented | `Features/Play` |
| 8 games: Banana Catch, Mood Match, Memory Flip, Brain Break, Word Puzzle, Calm Tap, Focus Game, Stress Smash. Each has full mechanics, scoring, best score and play again | Full | Implemented | `Features/Play/Games` |
| 4 quizzes, including the Couple quiz | Full | Implemented | `QuizView` |
| Achievements ("Moments"; no streaks) | Full | Implemented and tested | `CarePlayRepository` |

### Ask

| Feature | Android | iOS | Code |
|---|---|---|---|
| Ask home: six shortcuts, composer, personal-answers toggle, myth link | Full | Implemented | `Features/Ask` |
| Ask chat (chat, explain, myth) through the Worker `/v1/ai`; local history; on-device safety guard and safety block | Partial (needs the Worker and AI key) | Implemented | `AIService`, `AskChatView` |
| Appointment prep: local summary, AI questions, PDF share | Full; AI part partial | Implemented: PDF built with UIGraphicsPDFRenderer | `AppointmentPrepView` |

### Connect

| Feature | Android | iOS | Code |
|---|---|---|---|
| Invite codes, accept, permissions, relationship, disconnect, partner view, replies | Full (needs Firebase) | Implemented | `ConnectService`, `Features/Connect` |
| SOS sheet: push through the Worker, or the system share sheet | Full | Implemented | `SOSSheet` |
| Push notifications for Connect | Full (FCM data messages) | Implemented. **Needs the Worker change in this repo** (APNs alert for `platform: ios`) and an APNs key in Firebase | `AppDelegate`, `backend/worker/src/google.ts` |

### Wellbeing modes and settings

| Feature | Android | iOS | Code |
|---|---|---|---|
| I Need Something sheet | Full | Implemented | `NeedSomethingSheet` |
| Banana Mode (7 personas) and Today Is Not the Day | Full | Implemented | `BananaModeView`, `NotTheDayView` |
| Local reminders: period heads-up, evening check-in, quiet hours, discreet text | Full (WorkManager) | Implemented. Scheduled ahead with UNCalendarNotificationTrigger; see the sync doc for the iOS limits | `ReminderScheduler` |
| App lock: PBKDF2 PIN, relock after 1 minute in the background, 5-try cooldown | Full | Implemented; PIN hash tested | `LockView` |
| Privacy screen, appearance (System, Light, Warm dark), reduced motion, help and about | Full | Implemented | `Features/You` |
| Wipe this device | Full | Implemented | |
| Couple Mode | Not on Android (only the Couple quiz and the partner view exist) | Not ported as a separate mode | — |
| Data import | Not on Android | — | — |

## Cross-platform gaps found during the audit

These gaps need Android changes. Under the brief's rule 1, **I have not made them**; they are listed for your approval. Details are in `IOS_DATABASE_AND_SYNC.md`.

1. **Android only pulls from the cloud at sign-in.** A log made on iOS reaches Android only after the user signs in again on Android. iOS pulls on every launch and every time it returns to the foreground.
2. **Settings are not synced on Android.** Settings that live only in DataStore are:
   - cycle length and period length
   - regularity
   - name
   - Banana Mode
   - custom moods and custom symptoms
   - SOS messages
   - sleep routine
   - AI opt-in

   iOS writes and reads them in a `settings` map on `users/{uid}`, which the existing rules already allow. Android doesn't read that map yet.
3. **The kit seed is duplicated.** Android seeds 4 kit items with random IDs on every device, so a second device duplicates them after sync. iOS uses fixed seed IDs and removes its untouched seeds when the cloud already has kit items.
