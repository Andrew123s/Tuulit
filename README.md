# Tuulit

A menstrual health and wellness companion for Android. It covers cycle tracking with honest predictions, comfort for hard days, games, AI-backed answers, and optional sharing with a trusted person. It is local-first and private by default.

| Part | Where | Stack |
|---|---|---|
| Android app | `app/` | Kotlin, Jetpack Compose, MVVM, Room + SQLCipher, DataStore, WorkManager, Firebase (Auth, Firestore, FCM, App Check) |
| API | `backend/worker/` | Cloudflare Worker (TypeScript): AI proxy, Brevo email, R2 media, FCM sends, account deletion |
| Database rules | `firebase/` | Firestore security rules and emulator tests |
| Docs | `docs/` | Plan, status, architecture, schema, setup guides, security, testing, changelog |

## Quick start (local-only mode)

```
# Android SDK 36 and JDK 17 required
echo "sdk.dir=/path/to/Android/Sdk" > local.properties
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Without Firebase configuration the app runs fully offline: tracking, Care, Play and reminders all work. Account, Ask and Connect explain what is missing.

## Turning on the online features

1. Firebase: `docs/FIREBASE_SETUP.md`. Add `app/google-services.json`.
2. Worker and R2: `docs/CLOUDFLARE_R2_SETUP.md`.
3. Brevo: `docs/BREVO_SETUP.md`.
4. Add `TUULIT_API_BASE_URL=https://…workers.dev` to `local.properties` and rebuild.

See `docs/IMPLEMENTATION_STATUS.md` for what is done and tested.
