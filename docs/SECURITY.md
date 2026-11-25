# Security and privacy

Menstrual and reproductive health data is highly sensitive. These are the controls in place, and where each lives in the code.

## On the device

| Control | Where |
|---|---|
| Health database encrypted with SQLCipher. The 256-bit random passphrase is wrapped by an AES-GCM key in the Android Keystore. | `data/local/TuulitDatabase.kt` (`DatabaseKey`) |
| No system or cloud backups of app data (`allowBackup=false`, full exclusion rules) | `AndroidManifest.xml`, `res/xml/backup_rules.xml`, `res/xml/data_extraction_rules.xml` |
| App-lock PIN stored only as a PBKDF2-SHA256 hash (120k iterations, random salt). 30 s cool-down after 5 wrong tries. Re-locks after 60 s in the background. | `domain/Validation.kt` (`PinHasher`), `ui/auth/Auth.kt`, `MainActivity` |
| With app lock on, `FLAG_SECURE` keeps screens out of the recent-apps thumbnail and screenshots | `MainActivity` |
| Discreet notifications by default: lock-screen text is always "Take a quick moment for yourself", with a private public-version | `notifications/Notifications.kt` |
| No analytics or advertising ID | `AndroidManifest.xml` meta-data, no Analytics dependency |
| Cleartext HTTP disabled | `android:usesCleartextTraffic="false"` |
| CSV export neutralises spreadsheet formulas and is written only to the share cache | `TrackingRepository.exportCsv`, `FileProvider` |
| Voice notes stored in app-private storage. Microphone permission is asked for only when tapped. | `data/remote/MediaRepository.kt`, `ui/log/Voice.kt` |
| No personal screen without a signed-in account with a confirmed email. Local data belongs to one account (`session_uid`). Signing out backs up and then erases it from the phone. A different account signing in erases the previous account's data first. | `MainActivity` (auth gate), `AppContainer.onAuthChanged`, `AccountRepository.signOut` |

## In the cloud

| Control | Where |
|---|---|
| Firestore: every rule requires a signed-in user with a **confirmed email** (`email_verified`). `users/{uid}/**` is readable and writable only by that user. Field and size limits apply. Everything else is denied by default. | `firebase/firestore.rules`, 8 emulator tests |
| Connect: invites are one-time, expire in 24 h and can't be listed. Connections are created with every permission **off**, and only the owner can change permissions. The member can read only `shared/{id}`, and the owner can only write fields they have permitted. Either side can revoke. | `firebase/firestore.rules`, 7 emulator tests |
| Symptoms, moods, notes and journal text are **never** shareable | `ConnectRepository.publishStatus`, rules key allow-list |
| Every Worker route verifies the Firebase ID token (RS256, Google JWKS, audience, issuer, expiry). Ask, media and Connect also require `email_verified` (403, code `email_unverified`). Only the welcome email and account deletion accept unconfirmed accounts. App Check runs in monitor or enforce mode. | `backend/worker/src/jwt.ts`, `index.ts` |
| Per-user daily rate limits: AI 60, uploads 50, pushes 30, welcome email 1. Firebase rate-limits verification and reset emails. | `index.ts` (`limit`) |
| Secrets exist only as Worker secrets: AI key, Brevo key, Firebase service account, media signing key. None are in the app, the repo or the docs. A malformed key is refused with a `secret_invalid` log (name and length only). | `wrangler.toml` comments, `.gitignore`, `env.ts` (`requireSecret`) |
| R2 bucket is private. Access is only through 5-minute HMAC-signed URLs scoped to `users/{uid}/`. PUT and GET signatures are distinct. | `backend/worker/src/media.ts` |
| AI: the cycle summary is sent only with consent ("Personal answers in Ask"), and never includes notes, journal text or dates. Conversations are stored only on the device. Server-side safety re-check. The system prompt forbids diagnosis and contraception claims. | `AiRepository`, `backend/worker/src/ai.ts`, `safety.ts` |
| Account deletion requires recent re-authentication and deletes the Firestore backup, devices, connections, shared status, invites, R2 objects and the auth user, then wipes the device | `AccountRepository`, `backend/worker/src/index.ts` (`deleteAccount`) |
| Worker logs contain event names and status codes only, never emails, messages or health data | `backend/worker/src/*.ts` |

## Secrets checklist before pushing to GitHub

- `app/google-services.json`, `local.properties`, `.env*`, keystores, `node_modules/` and `.wrangler/` are git-ignored.
- Search before committing:
  ```
  git grep -nE "AIza|sk-|xkeysib|BEGIN PRIVATE KEY"
  ```
  This should return nothing.

## Known limitations

- If the Android Keystore key is lost (for example after some device restores), the local database can't be decrypted. The app starts fresh, and the cloud backup restores on sign-in.
- Sync is last-write-wins per row. Editing the same day on two phones at once keeps the later edit.
- App Check stays in `monitor` until Play Integrity is registered. Then switch the Worker to `enforce` and turn on Firestore enforcement.
