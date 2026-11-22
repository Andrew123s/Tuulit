# Architecture

## Overview

```
 Android app (Kotlin, Compose)                      Cloudflare (free plan)             Firebase (Spark)
 ┌──────────────────────────────┐   ID token +      ┌───────────────────────┐   admin    ┌────────────────────┐
 │ UI (Compose, design system)  │   App Check       │ Worker  tuulit-api    │ ─────────▶ │ Firestore (REST)   │
 │ ViewModels / screen state    │ ────────────────▶ │  /v1/ai      ─▶ AI provider        │ FCM HTTP v1        │
 │ Repositories                 │                   │  /v1/email/* ─▶ Brevo  │           │ Identity Toolkit   │
 │  ├ Room + SQLCipher (truth)  │                   │  /v1/media/* ─▶ R2     │           └────────────────────┘
 │  ├ DataStore (settings)      │                   │  /v1/connect/notify    │                    ▲
 │  ├ Firebase Auth / Firestore │ ─────── direct (rules-enforced) ───────────────────────────────┘
 │  └ WorkManager (reminders)   │                   └───────────────────────┘
 └──────────────────────────────┘
```

- **Local-first.** Room, encrypted with SQLCipher, is the source of truth. Every feature except backup, Ask and Connect works offline and without an account.
- **Firebase Spark only:** Authentication (email and password), Firestore, Cloud Messaging and App Check. There are no Cloud Functions, because they need the Blaze plan.
- **One Cloudflare Worker** does every server-side job that needs a secret: AI, Brevo, R2, FCM sends and admin deletion. It verifies the Firebase ID token on every request.

## Android packages (`app/src/main/java/app/tuulit`)

| Package | Responsibility |
|---|---|
| `domain` | Pure Kotlin. `CycleCalculator` (predictions, insights, pain patterns), `MedicalSafetyGuard`, `Validation`, `PinHasher`, `SuggestionEngine`, `TodayHeroBuilder`, Care/Play content. No Android imports, so it is fully unit-tested. |
| `data/local` | Room entities, DAO and encrypted database (`TuulitDatabase`, `DatabaseKey`), and `PreferencesRepository` (DataStore). |
| `data` | `TrackingRepository` (cycles, logs, pain, export), `CarePlayRepository` (library, sessions, scores, achievements), `CycleState` (the shared snapshot every screen reads), `AccountRepository`. |
| `data/remote` | `Firebase` (optional at runtime), `AuthRepository`, `SyncRepository` (Firestore backup), `ApiClient` (Worker), `AiRepository`, `ConnectRepository`, `MediaRepository` (R2 voice notes). |
| `notifications` | Channels and discreet posting, `ReminderRules` and `ReminderWorker` (local WorkManager reminders), `TuulitMessagingService` (FCM). |
| `ui/designsystem` | Compose implementation of the approved design system: tokens (`Theme.kt`), icons generated from the design SVGs, the vector mascot, components, cycle ribbon and calendar. |
| `ui/<feature>` | `onboarding`, `auth`, `today`, `log`, `cycle`, `care`, `play`, `ask`, `connect`, `you`, and `nav` (navigation graph). |
| `TuulitApp` / `AppContainer` | Manual dependency container: one instance of each repository per process. |

**Patterns.** Screens follow MVVM, with a ViewModel where a screen holds working state (Today, the log composer, Ask). Simpler screens read the shared `CycleState` snapshot and call repositories through the container. Coroutines and Flow are used throughout. Business rules live in `domain` and the repositories, never in composables.

## Key decisions

1. **Room is the source of truth, and Firestore is a backup.** The app is useful offline and without an account, and Firestore reads stay low. Sync pushes rows changed since the last sync (tombstones included) in batches of up to 400. The first sign-in on a device pulls everything and merges last-write-wins by `updatedAt`.
2. **The database is encrypted.** SQLCipher uses a random 256-bit passphrase, wrapped by an AES-GCM key in the Android Keystore. System backups are disabled (`allowBackup=false` plus exclusion rules), so health data never reaches Google Drive backups.
3. **The PIN is hashed with PBKDF2-SHA256**, using 120,000 iterations and a random salt. The app locks again after 60 seconds in the background. `FLAG_SECURE` keeps screens out of the recent-apps thumbnail.
4. **The Worker replaces Cloud Functions.** Spark has no functions, so the Worker holds the AI key, the Brevo key, the Firebase service account and the media signing key. The app only sends a short-lived Firebase ID token, plus an App Check token when available.
5. **The AI provider is configurable.** `AI_PROVIDER` (`anthropic` or `openai_compatible`) and `AI_MODEL` are Worker variables. Switching providers needs no app release. Conversations are stored only on the device. The cycle summary is sent only when "Personal answers in Ask" is on, and it never includes notes, journal text or entry dates.
6. **Safety is checked twice.** `MedicalSafetyGuard` (Kotlin) and `safety.ts` (Worker) apply the same rules. A red flag shows a medical notice, with call and SOS actions, before any answer.
7. **Connect needs no server for permissions.** Invites, connections and shared status are Firestore documents protected by rules. A member can only ever read `shared/{connectionId}`, which holds only the fields the owner permitted. Pushes go through the Worker, which re-checks the connection and its permission.
8. **Voice notes go through Worker-signed URLs.** R2 credentials never reach the device. The Worker issues 5-minute HMAC-signed URLs to itself and streams bytes to and from the R2 binding. Keys are always `users/{uid}/…`.
9. **Reminders are scheduled on the device.** Period and check-in reminders are computed and scheduled by WorkManager, so they need neither FCM nor a server, and they respect quiet hours and discreet mode.
10. **No analytics.** Firebase Analytics is not included, and collection flags are off in the manifest.
