# Firebase setup (Spark plan)

Tuulit uses only Spark-plan services: Authentication, Cloud Firestore, Cloud Messaging and App Check. **Do not enable Cloud Functions or Storage, and do not upgrade to Blaze.** The Cloudflare Worker does the server work.

Until these steps are done, the app runs in local-only mode. Tracking, Care, Play and reminders work; Account, Ask and Connect explain that accounts aren't set up yet.

## 1. Create the project

1. Go to https://console.firebase.google.com and click **Add project**. Name it (for example `tuulit-prod`). Turn **Google Analytics off**; Tuulit doesn't use it.
2. Confirm the plan shows **Spark** under Usage and billing.

## 2. Register the Android app

1. Go to Project settings → Your apps → **Add app** → Android. Package name: `app.tuulit`.
2. Add the SHA-256 fingerprints of your signing keys (debug: `./gradlew signingReport`; release: from your upload key or Play App Signing). App Check needs these.
3. Download `google-services.json` and put it at **`app/google-services.json`**. It is git-ignored. It holds only public identifiers, but it stays out of the repo so each environment can use its own project.

## 3. Authentication

1. Go to Build → Authentication → Get started → Sign-in method → enable **Email/Password**. Leave "Email link" off.
2. Under Settings → User actions, keep "Email enumeration protection" **on**.
3. Under Templates, set the sender name to Tuulit and adjust the **Email address verification** and **Password reset** templates if you like. Tuulit uses these built-in Firebase emails for both. It is the only verification path (see `docs/AUTH_AND_SESSION.md`).

## 4. Firestore

1. Go to Build → Firestore Database → Create database → **Production mode**. Pick a region close to your users (for example `europe-west`).
2. Deploy the rules from this repo:
   ```
   cd firebase
   npm install
   npx firebase login
   npx firebase use --add          # choose your project
   npx firebase deploy --only firestore:rules,firestore:indexes
   ```
   Every rule requires `request.auth.token.email_verified == true`, so an unconfirmed account can't read or write anything.
3. Run the rules tests locally against the emulator. This needs Java 11+:
   ```
   cd firebase && npm test
   ```

## 5. Cloud Messaging

FCM is enabled by default. The Worker sends with the **HTTP v1 API** using the service account (step 7), so no legacy server key is needed.

## 6. App Check

1. Go to Build → App Check → Apps → your Android app → **Play Integrity** → register (requires the SHA-256 from step 2).
2. Debug builds use the debug provider. Run the app once, copy the debug token from Logcat (`DebugAppCheckProvider`) and add it under App Check → Manage debug tokens.
3. Leave enforcement for Firestore **off** until you've confirmed real traffic passes, then turn it on.
4. In the Worker, set `APP_CHECK_MODE` from `monitor` to `enforce` once Play Integrity works in production.

## 7. Service account for the Worker

1. Go to Project settings → Service accounts → **Generate new private key**. This downloads a JSON file. Treat it as a password.
2. Store it only as a Worker secret:
   ```
   cd backend/worker
   npx wrangler secret put FIREBASE_SERVICE_ACCOUNT   # paste the whole JSON
   ```
3. Delete the downloaded file. Never commit it.

The Worker uses it for:
- Firestore REST (reading a connection before sending a push, deleting an account's data)
- FCM v1 sends
- Identity Toolkit (deleting the auth user)

All of these are free on Spark.

## 8. Values the Worker needs

In `backend/worker/wrangler.toml` set:
- `FIREBASE_PROJECT_ID`: Project settings → General → Project ID.
- `FIREBASE_PROJECT_NUMBER`: Project settings → General → Project number. Used to verify App Check tokens.
