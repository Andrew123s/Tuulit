# Authentication, sessions and email verification (Android 2.3.0)

This document records how sign-in, email confirmation, sign-out and per-account data isolation work, and why the 2.3.0 changes were made.

## Root causes fixed in 2.3.0

| Problem reported | Root cause found | Fix |
|---|---|---|
| The app opened without login or registration | By design until 2.2.0: Tuulit was local-first and the account was optional. `MainActivity` routed on `onboardingCompleted` only and never looked at the auth state. It was not a hidden bypass; there was simply no gate. | `MainActivity` now gates every personal screen on Firebase Auth (see the flow below). |
| The profile stayed visible after logout | "Sign out" only called `FirebaseAuth.signOut()`. The encrypted Room database, DataStore settings, voice notes, the FCM device registration and scheduled reminders all stayed, and the app kept showing them. | A full sign-out sequence (below) and per-account ownership of local data. |
| Ask showed "The answer service is unavailable right now" | The Worker secret `AI_API_KEY` held one character, `\x16` (Ctrl+V). A paste into `wrangler secret put` failed, and Gemini answered every request with HTTP 400 and an empty body. A second problem: Gemini 3 "thinking" models spend output tokens on reasoning, so `max_tokens: 700` cut answers off mid-sentence. | The key was re-entered. The Worker now refuses malformed secrets with a clear `secret_invalid` log, sends `reasoning_effort: "low"` with a 4096-token budget, and logs `ai_truncated` if an answer is still cut off. |
| The verification email wasn't sent | Two competing paths. The app called the Worker's `/v1/email/verification` (Brevo) and fell back to Firebase only if that failed. `BREVO_API_KEY` was also a single Ctrl+V character, so Brevo returned 400 for every email; the Firebase fallback then hit Firebase's rate limit on "Send again" ("Too many attempts"). | **One** verification path: Firebase Authentication's built-in `sendEmailVerification`. The Worker's verification route and Brevo template were removed. Brevo still sends the welcome and account-deleted emails; the Brevo key was re-entered. |

## Flow

```
App start
 ├─ Firebase not configured in this build → "Tuulit can't sign you in" (no way past)
 ├─ Signed out → Welcome → Create account | Sign in (forgot password inside)
 ├─ Signed in, email not confirmed → Verify-email screen
 ├─ Signed in and confirmed
 │    ├─ first time on this phone → "Getting your Tuulit ready…" (full restore)
 │    │     └─ restore failed → "Try again" | "Continue without restoring"
 │    ├─ App lock (PIN) if set
 │    ├─ Onboarding (last period → cycle basics → privacy) unless the cloud already had data
 │    └─ Home
```

`AuthRepository.state` follows Firebase's **ID-token listener**. That listener fires on sign-in, sign-out and token refresh, so a confirmed email (picked up by `refresh()`, which reloads the user and forces a new token) moves the person on without signing in again. A disabled or deleted account fails `reload()` and is signed out.

### Verify-email screen

- Shows the registered address and tips: check spam and promotions, search for "Tuulit", the link works on any device.
- **I've confirmed my email** re-checks now and says "Not confirmed yet…" if not.
- **Send the email again** uses Firebase's sender, with a 60-second cooldown and Firebase's own rate limit ("Too many attempts. Please wait a few minutes.").
- It re-checks automatically every 10 seconds and whenever the app returns to the foreground.
- **Use a different account** signs out. It can't loop: an unconfirmed account never owns or sees local data, so there is nothing to back up or erase.

## Per-account local data

The phone's data belongs to one Firebase uid, stored in DataStore as `session_uid` (`AppContainer.onAuthChanged`).

| Situation | What happens |
|---|---|
| Data from before 2.3.0 (no `session_uid`) | Adopted by the first confirmed account that signs in, then backed up. Existing users keep everything. |
| Same account signs in again on the same phone with onboarding done | Opens at once; the cloud copy merges in the background. |
| A different confirmed account signs in | The previous account's local data is erased **before** the new account's backup is restored. |
| Unconfirmed account | Claims nothing and sees nothing. |

### Sign-out sequence (`AppContainer.signOut` → `AccountRepository.signOut`)

1. Back up pending changes. If that fails (usually offline), nothing is erased; the sheet explains what would be lost and offers "Sign out anyway".
2. Delete this phone's `users/{uid}/devices/{token}` document and the FCM token, so a signed-out phone gets no Connect pushes.
3. `FirebaseAuth.signOut()`.
4. Erase the encrypted database (cycles, logs, pain, comfort, AI conversations, games, achievements), voice notes, cache, scheduled work and shown notifications. Settings are cleared except theme and reduced motion. The PIN goes with them.
5. Reset the in-memory lock and session state. The navigation back stack goes with the main shell, so Back can't return to the profile.

**The account and its Firestore data are never deleted by sign-out.** Deleting the account is the separate "Delete account" flow.

## Server-side enforcement

The app's gate is not the only check:

- **Firestore rules**: `signedIn()` requires `request.auth.token.email_verified == true`, so every rule (private backup, invites, connections, shared status) refuses unconfirmed accounts. The emulator test "refuses a signed-in user whose email isn't confirmed" covers it (8 tests in total).
- **Worker**: Ask, media and Connect return `403 {code: "email_unverified"}` for unconfirmed tokens. Only the welcome email and account deletion accept them.

## Error messages in the app

`ApiClient` turns every failure into a readable `ApiError`:

| Case | Message (shortened) |
|---|---|
| No network | You're offline. Check your connection… |
| Timeout | That took too long to answer… |
| 401 | Your sign-in has expired. Please sign in again. |
| 403 `email_unverified` | Confirm your email address to use this… |
| 429 | That's a lot of questions… (or the Worker's limit message) |
| 502/503/504 | The Worker's message, for example "Ask is very busy right now" (provider 429), "The answer service is temporarily unavailable" (provider 5xx), "Ask is temporarily unavailable while we fix a problem on our side" (bad key, model or endpoint), "Ask isn't set up on our server yet" (missing or malformed key) |
| Reply that isn't JSON, or an empty answer | We got an unexpected reply from our server… |

The app never shows a made-up answer.
