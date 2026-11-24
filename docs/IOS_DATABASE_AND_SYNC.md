# iOS database and sync

The iPhone app is written to the same Firestore project, collections, document IDs and field types as Android. There is no second database and no iOS-only backend.

## Shared Firestore schema

This is the existing schema; nothing new is required.

### `users/{uid}`

The user's private root document. The rules allow only that user to read or write it.

| Field | Written by | Contents |
|---|---|---|
| `lastBackupAt` | both platforms | server timestamp |
| `settings` | both platforms | map: see "Settings sync" below |

### Subcollections of `users/{uid}`

| Subcollection | Document ID | Fields |
|---|---|---|
| `cycles/` | UUID (lowercase) | `startEpochDay` (int), `endEpochDay` (int or null), `updatedAt` (ms), `deleted` |
| `dailyLogs/` | epoch day as a string | `epochDay`, `flow` (enum name), `symptoms` (`"Cramps:2\|Bloating:1"`), `moods` (`"Calm\|Tired"`), `moodScore`, `energyLevel`, `sleepQuality`, `waterGlasses`, `exerciseNote`, `medicationNote`, `journalText`, `voiceNoteKey`, `updatedAt`, `deleted` |
| `painLogs/` | UUID | the PainLog fields, plus `createdAt`, `updatedAt`, `deleted` |
| `comfort/` | UUID or `seed-kit-N` | `kind`, `title`, `detail`, `icon`, `checked`, `helpedCount`, `sessionId`, `position`, `updatedAt`, `deleted` |
| `games/` | game ID | `bestScore`, `plays`, `updatedAt` |
| `achievements/` | achievement code | `unlockedAt` |
| `devices/` | last 64 characters of the FCM token | `token`, `platform` (`android` or `ios`), `updatedAt` |

### Top-level collections

These are unchanged and shared by both platforms: `invites/{code}`, `connections/{owner_member}` and `shared/{connId}`.

### Types and IDs that must match exactly

All of these are implemented in `TuulitCore` and unit-tested.

- **Epoch days.** These are *local calendar dates* counted from 1970-01-01. This is the same value as Java's `LocalDate.toEpochDay()`. iOS reads the year, month and day from `Calendar.current` and converts them with the days-from-civil algorithm, so 23 Nov 2026 is the same integer on both phones. Timestamps are never converted to days in UTC.
- **Integers.** They are written as Swift `Int`, which Firestore stores as `integerValue`; Android reads them with `getLong`. Doubles are never written.
- **Symptom and mood encoding.** This is byte-for-byte the same as `DailyLogEntity.encodeSymptoms` and `decodeSymptoms`.
- **Enum names.** `FlowIntensity` and `CycleRegularity` are stored by their Kotlin `name` strings: `MEDIUM`, `SOMEWHAT_VARIABLE` and so on.

## Merge rules

These are ported from Android's `SyncRepository` and live in `TuulitCore/SyncMerge.swift`.

- **Push.** Every row with `updatedAt > lastSyncAt` is written, in batches of up to 400. Tombstones (`deleted: true`) are pushed like any other change.
- **Pull.** For each remote row, the copy with the newer `updatedAt` wins (last write wins).
- **Cycles.**
  - Cycle start dates are unique, and tombstones still hold their start date. This matches the Android Room unique index.
  - When a different local cycle already owns the remote cycle's start date, the newer of the two wins.
  - The loser is tombstoned and its start date is moved to `-start - 1`, exactly as on Android.
- **Games.** The best score and the play count each take the maximum of the two copies.
- **Achievements.** The union of both sides is kept.

## When iOS syncs

- **On sign-in:** a full pull, then a push.
- **On launch and on every return to the foreground (while signed in):** a full pull, then a push. Android currently pulls only at sign-in; see gap 1.
- **After a local change:** a debounced push 4 seconds later, as on Android.
- **Manually:** "Back up now" in Account.

## Offline behaviour

- The local file store is the source of truth. Logging never waits for the network.
- Unsynced changes are simply rows with `updatedAt > lastSyncAt`, so there is no separate queue that could be lost. They go up on the next successful sync.
- Firestore's own offline cache is turned **off** for user data. The local store is the cache, and this avoids two caches disagreeing.
- Duplicates are prevented by three things:
  - Daily logs are keyed by epoch day.
  - Cycles are protected by the unique start-date rule.
  - Games and achievements are keyed by ID.
- A connection banner appears in Account and Connect when the network monitor (`NWPathMonitor`) reports no connection.

## Cross-platform gaps (resolved 2026-11-24)

All four changes are now made and tested. Items 1 to 3 were approved by you on 2026-11-24.

1. **Android pulls only at sign-in.** In `TuulitApp.start()`, a full pull runs only when the auth state changes. Proposed fix: also call `sync.sync(fullPull = true)` in `onStart` of the process lifecycle while signed in. Until then, records made on iOS appear on Android only after the user signs in again there.
2. **Settings sync.** iOS mirrors these settings to `users/{uid}.settings`:
   - `displayName`, `cycleLength`, `periodLength`, `regularity`
   - `bananaMode`, `customMoods`, `customSymptoms`
   - `sosMessages`, `sleepNote`, `aiContextOptIn`
   - `updatedAt`

   The rules already permit this. Android needs the matching read and write in `PreferencesRepository` and `SyncRepository`. Device-only settings stay local on both platforms: theme, PIN, reminders, quiet hours and discreet mode.
3. **Kit seed.** Android's random-ID seed duplicates the kit on a second device. Proposed fix: use the fixed `seed-kit-0` to `seed-kit-3` IDs that iOS uses.
4. **Pushes to iPhones.** The Worker sent data-only FCM messages, which iOS does not display. **This change is in this repo:** `backend/worker/src/google.ts` now adds an APNs alert block when the device document says `platform: "ios"`. Android messages are unchanged. You also need to upload an APNs auth key in the Firebase console.

## Data deletion and export

- **Delete account.** This uses the same Worker endpoint, `/v1/account/delete`. If the Worker isn't deployed, the app runs the same client-side fallback as Android.
- **Delete cloud backup.** The same collection sweep as Android.
- **Wipe this device.** This deletes the store file, the voice notes, the settings and the Keychain PIN hash.
- **Export.** The CSV format is the same as Android's (with formula injection neutralised) and is shared through `ShareLink`.

## Ask conversations

Ask conversations stay on the device on both platforms, as a privacy choice. They are not synced, so iOS and Android each keep their own history. Only the question being asked is sent, plus the consented summary when "Personal answers" is on.
