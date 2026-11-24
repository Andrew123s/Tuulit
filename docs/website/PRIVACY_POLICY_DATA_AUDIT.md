# Privacy policy data audit

Audited from the source code on 2026-11-24. Files checked:
- `app/` (Android 2.2.0)
- `ios/`
- `backend/worker/src`
- `firebase/firestore.rules`
- the live `tuulit-prod` project configuration

The published policy at `/privacy` (`website/src/pages/privacy.astro`) is written from this audit. When the app's data practices change, update this file first, then the policy, then its version and date in `website/src/data/site.ts`.

## What the app collects, and where it goes

### On the phone only (never received by Ycomspace)

| Data | Storage | Code |
|---|---|---|
| Cycles, daily logs (flow, mood, symptoms, energy, sleep, water, medication, movement, journal), pain entries, comfort and kit, game scores, achievements | SQLCipher database, key in Android Keystore. iOS: Data Protection (complete) | `data/local/*`, `TuulitStore` |
| Ask conversation history | Same database; never synced | `AiMessageEntity` |
| Voice notes | App-private files | `MediaRepository` |
| Settings and PIN | DataStore. The PIN is a PBKDF2 hash (120k iterations, salted) | `PreferencesRepository` |
| Backups | `android:allowBackup="false"`. iOS: excluded from backup | `AndroidManifest.xml`, `FileBackend` |

### Sent to services, only when the person uses a feature

| Feature | Data | Destination | Code |
|---|---|---|---|
| Account | Email, password (Firebase hashes it), display name, uid | Firebase Auth | `AuthRepository` |
| Backup (automatic when signed in) | Everything in the on-device list **except** Ask history and voice audio. Also the synced settings map (name, cycle and period length, regularity, Banana Mode, custom moods and symptoms, SOS messages, sleep note, AI opt-in) and `lastBackupAt` | Firestore `users/{uid}/…`, database location `eur3` (Europe multi-region) | `SyncRepository` |
| Push | FCM token, platform, timestamp | Firestore `users/{uid}/devices` | `ConnectRepository.registerDevice` |
| Connect | Invite (code, owner uid and name, status, `createdAt`, `expiresAt`, `acceptedBy`); connection (both uids and names, relationship, permissions, `inviteCode`); shared status (`periodStartedOn`, `periodSoonOn`, `phase`, `roughDayAt`, `lastNote` of 140 characters or fewer, names, uids) | Firestore `invites`, `connections`, `shared` | `ConnectRepository`, `firestore.rules` |
| Connect push content | `fromName`, `message` (140 characters or fewer), type, connection id | Worker → FCM → APNs or Android | `connect.ts` |
| Ask | Question, up to 12 recent messages (each truncated to 2,000 characters), locale. Optional summary: cycle day, phase, average cycle and period length, variability, top 4 symptoms, number of severe-pain cycles | Worker → Google Gemini API (`AI_PROVIDER=openai_compatible`, `generativelanguage.googleapis.com`) | `AiRepository`, `ai.ts` |
| Emails | Email address, display name | Worker → Brevo (welcome, verification, deletion confirmation). Password reset is sent by Firebase itself | `email.ts`, `AuthRepository` |
| Voice backup (opt-in per recording) | `.m4a` file, key `users/{uid}/voice/{uuid}.m4a` | Cloudflare R2 `tuulit-media` | `media.ts` |
| App Check | Play Integrity or App Attest token | Firebase App Check (Worker in `monitor` mode) | `Firebase.kt` |
| Rate limits | `ai:{uid}:{date}` counter, `contact:{ip}:{date}` counter, each with a 26-hour TTL | Cloudflare KV | `index.ts`, `contact.ts` |
| Worker logs | Event names only: `ai` (tool, urgency, withContext), `brevo_sent` (subject), `account_deleted` (object count), errors. **No content and no email addresses** | Cloudflare Workers observability | `index.ts`, `email.ts` |
| Dictation | Audio handled by the OS speech recogniser. Android prefers offline; iOS requires on-device when supported | Google or Apple, under their terms | `Voice.kt`, `VoiceJournal.swift` |
| Website contact form | Name, email, topic, message; IP address (KV counter) | Worker → Brevo → `ycomspacegh@gmail.com` | `contact.ts` |

### Not present (verified)

- Firebase Analytics, Crashlytics, Performance and any other analytics or crash SDK: none in `app/build.gradle.kts`.
- Advertising IDs, location, contacts, photos: none. The only Android permissions are INTERNET, POST_NOTIFICATIONS and RECORD_AUDIO.
- The website sets no cookies and loads no third-party scripts.

## Deletion (verified in code)

- **In-app:** You → Account and backup → Delete account. The app re-authenticates with the password, then calls the Worker at `/v1/account/delete`. That endpoint requires a sign-in less than 10 minutes old. It deletes:
  - every `users/{uid}` subcollection and the root document;
  - connections and shared status, where the person is owner or member;
  - the person's invites;
  - every R2 object under `users/{uid}/`;
  - the Firebase Auth user.

  It then sends a confirmation email and wipes the phone.
- **Fallback when the Worker isn't deployed:** a client-side delete within what the rules allow.
- **Deleted entries (changed today):** tombstones now keep only the date or id. Content is cleared on both Android and iOS. Deleted cycles keep their start and end dates, because those are needed to resolve sync conflicts.
- **A backed-up voice note stays in R2** after its day is cleared, until account deletion. A possible improvement: delete the R2 object when the day is cleared.

## Owner checklist: required before publishing and Play submission

These can't be filled in from the code. Nothing below has been invented on the website.

1. **Impressum (German legal notice, § 5 DDG).** A German operator's website and app listing need a legal notice with the operator's full name or company name, a **postal address** (a P.O. box isn't enough), and an email address. Add the details and I'll build `/legal-notice` and link it from the footer.
2. **Company form.** "Ycomspace" is used as the operator name. If it's a registered company (for example UG or GmbH), confirm the exact legal name, register court and number, and VAT ID if any.
3. **Gemini API data use.** Confirm the API key is on a **paid (billing-enabled) project**. On the Gemini API's unpaid tier, Google may use prompts and responses to improve its products, and human reviewers may read them. That isn't acceptable for health questions and would have to be disclosed in the policy.
4. **Data processing agreements.** Accept and keep records of:
   - Google Cloud/Firebase Data Processing Terms (Firebase console → Project settings → Privacy);
   - Cloudflare's DPA;
   - Brevo's DPA.
5. **Explicit consent in the app.** Health data in the cloud relies on Art. 9(2)(a) explicit consent. Account creation, "Personal answers in Ask" and Connect should each show a clear consent statement or checkbox, with a link to the policy, before the first cloud processing. **This isn't in the app yet and should be added before release.**
6. **Minimum age.** The policy and terms say 16+, the GDPR Art. 8 age in Germany. Confirm.
7. **Brevo sender.** `hello@tuulit.app` must be a verified sender in Brevo (domain authentication with DKIM and SPF records on `tuulit.app`). Otherwise account emails and contact-form forwarding will fail. See `NAMECHEAP_DNS_SETUP.md`.
8. **Support address.** `ycomspacegh@gmail.com` is published as the contact. Optional: create `support@tuulit.app` (for example with Cloudflare Email Routing) and update `site.ts`.
9. **Cloudflare settings.** Keep Bot Fight Mode and Web Analytics off for tuulit.app, or update the "no cookies, no analytics" sentence in the policy.
10. **Legal review.** Have the policy and terms reviewed by a qualified lawyer, particularly the international transfers paragraph, legal bases and governing law. The terms deliberately contain no governing-law clause.

## Google Play Data Safety form (draft answers)

The privacy policy doesn't complete this form. Enter these answers in Play Console → App content → Data safety:

- **Does the app collect or share user data?** Yes, it collects data. It doesn't share data with third parties for their own purposes. Service providers that process data on our behalf don't count as "sharing".
- **Is all data encrypted in transit?** Yes.
- **Can users request deletion?** Yes, in the app, with `https://tuulit.app/account-deletion` as the web link.
- **Data types collected:**

| Category | Type | Collected | Optional? | Purpose |
|---|---|---|---|---|
| Personal info | Email address | Yes | Optional (account) | Account management, App functionality |
| Personal info | Name | Yes | Optional | App functionality |
| Personal info | User IDs | Yes | Optional (account) | Account management, App functionality |
| Health and fitness | Health info (cycle, symptoms, pain, medication notes) | Yes | Optional (backup, Ask summary) | App functionality |
| Messages | Other in-app messages (Connect SOS and replies) | Yes | Optional | App functionality |
| Audio | Voice or sound recordings | Yes | Optional (voice backup) | App functionality |
| App info and performance | none | No | | |
| Device or other IDs | Device or other IDs (FCM token) | Yes | Optional (account) | App functionality |

- **Processed ephemerally:** Ask questions sent to the AI provider. They aren't stored on our server.
- **Health apps declaration:** in Play Console, also complete **App content → Health apps**, and declare the app as menstrual health / period tracking.
