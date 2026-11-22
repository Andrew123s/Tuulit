# Database schema

## Local: Room (`tuulit.db`, SQLCipher-encrypted, version 1)

The exported schema is in `app/schemas/`. Every synced table has `updatedAt` (epoch ms) and a `deleted` tombstone.

| Table | Key | Columns | Notes |
|---|---|---|---|
| `cycles` | `id` (UUID) | `startEpochDay` (unique), `endEpochDay?`, `updatedAt`, `deleted` | Derived from logged flow (Light or heavier) and onboarding. Spotting never starts a cycle. |
| `daily_logs` | `epochDay` | `flow`, `symptoms` ("Name:level\|…"), `moods` ("a\|b"), `moodScore` 0–5, `energyLevel` 0–5, `sleepQuality` 0–5, `waterGlasses`, `exerciseNote`, `medicationNote`, `journalText` (≤4000), `voiceNoteKey?`, `updatedAt`, `deleted` | One row per day. An empty log becomes a tombstone. |
| `pain_logs` | `id` (UUID) | `epochDay`, `intensity` 1–10, `location`, `painType`, `durationHours`, `medicationTaken`, `impactedWorkOrStudy`, `impactedExercise`, `impactedSleep`, `notes`, `createdAt`, `updatedAt`, `deleted` | |
| `comfort_items` | `id` (UUID) | `kind` ("comfort" or "kit"), `title`, `detail`, `icon`, `checked`, `helpedCount`, `sessionId?`, `position`, `updatedAt`, `deleted` | Personal Comfort Library and Survival Kit. |
| `care_completions` | `id` | `sessionId`, `completedAt`, `fullyCompleted` | Local only. |
| `achievements` | `code` | `unlockedAt` | Earned by real actions only. |
| `game_scores` | `gameId` | `bestScore`, `plays`, `updatedAt` | Personal bests. |
| `ai_messages` | `id` | `conversationId`, `role`, `text`, `tool`, `urgency`, `usedContext`, `failed`, `createdAt` | **Never synced.** Stays on the device. |

Settings live in DataStore (`tuulit_settings`): cycle defaults, theme, Banana Mode, notification preferences, PIN hash, SOS messages and so on.

## Cloud: Firestore

| Path | Written by | Read by | Contents |
|---|---|---|---|
| `users/{uid}` | owner | owner | `lastBackupAt` |
| `users/{uid}/cycles/{id}` | owner | owner | Mirror of `cycles` |
| `users/{uid}/dailyLogs/{epochDay}` | owner | owner | Mirror of `daily_logs` |
| `users/{uid}/painLogs/{id}` | owner | owner | Mirror of `pain_logs` |
| `users/{uid}/comfort/{id}` | owner | owner | Mirror of `comfort_items` |
| `users/{uid}/games/{gameId}` | owner | owner | Best score and plays |
| `users/{uid}/achievements/{code}` | owner | owner | `unlockedAt` |
| `users/{uid}/devices/{tokenSuffix}` | owner | Worker (admin) | FCM token, used only for Connect pushes |
| `invites/{code}` | owner (create), invitee (accept once) | anyone signed in, by exact code | `ownerUid`, `ownerName`, `status`, `expiresAt` (24 h), `acceptedBy` |
| `connections/{ownerUid}_{memberUid}` | invitee (create, all permissions off), owner (permissions, relationship) | owner and member | `ownerName`, `memberName`, `relationship`, `permissions{periodSoon,periodStarted,roughDay,phase}` |
| `shared/{connectionId}` | owner (only permitted fields) | owner and member | `periodStartedOn?`, `periodSoonOn?`, `phase?`, `roughDayAt?`, `lastNote?` |

No symptoms, moods, notes or journal text are ever written outside `users/{uid}`. The rules are in `firebase/firestore.rules` and tested in `firebase/test/rules.test.js`.

### Spark-plan budget (per day: 50k reads, 20k writes, 20k deletes, 1 GiB stored)

- A typical user writes 5–20 documents a day (debounced 4 s, changed rows only).
- A full restore reads every backed-up document once, per device sign-in.
- Connect listeners read 1 document per change.

At these rates Spark comfortably supports roughly 1,000 daily active users. Monitor usage in the Firebase console under Usage and billing. The app never upgrades the plan.

## Object storage: Cloudflare R2 (`tuulit-media`, private)

| Key | Contents |
|---|---|
| `users/{uid}/voice/{uuid}.m4a` | Voice-journal recordings the user chose to back up (≤10 MB each) |

The object key is stored in `daily_logs.voiceNoteKey`. Account deletion removes every object under `users/{uid}/`.
