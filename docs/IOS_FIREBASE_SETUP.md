# iOS Firebase setup

The iPhone app uses the **same** Firebase project as Android (`tuulit-prod`). You add an iOS app to that project. You don't create a new project or a second database. Once that's done, the same account works on both phones, and the data syncs between them.

Already done and verified on 2026-11-24:
- **Firestore:** rules deployed and identical to `firebase/firestore.rules` (database in `eur3`).
- **Sign-in:** email and password is enabled.
- **Worker:** live at `https://tuulit-api.mybeatstar.workers.dev`. `FIREBASE_SERVICE_ACCOUNT` is set from the `tuulit-prod` admin key.

## 1. Register the iOS app (about 5 minutes)

1. Open the [Firebase console](https://console.firebase.google.com/) and select **tuulit-prod**.
2. Go to ⚙ **Project settings** → **General** → **Your apps** → **Add app** → **iOS+**.
3. Fill in:
   - **Apple bundle ID:** `app.tuulit` (it must be exactly this).
   - **App nickname:** `Tuulit iOS`.
   - **App Store ID:** leave empty for now.
4. Click **Register app**, then **Download GoogleService-Info.plist**.
5. Put the file at `ios/Tuulit/Resources/GoogleService-Info.plist`.
   - It's gitignored, the same way `google-services.json` is for Android. Don't commit it.
   - For the GitHub Actions build you don't need it: without it, the app runs in local-only mode.
6. Skip the console's "Add Firebase SDK" and "Add initialization code" steps. The project already includes them.

Without this file, the app still runs fully on the phone, but accounts, backup, Ask and Connect explain that they aren't set up. Android behaves the same without its file.

## 2. Push notifications for Connect (APNs key)

iPhones receive Connect messages (SOS, replies, invite accepted) through Firebase Cloud Messaging. FCM can only deliver to iPhones once it has an Apple Push key.

1. Go to the [Apple Developer portal](https://developer.apple.com/account/resources/authkeys/list) → **Keys** → **+**.
2. Name the key `Tuulit APNs`, tick **Apple Push Notifications service (APNs)**, then **Continue** → **Register**.
3. Download the `.p8` file. Apple lets you download it only once, so keep it somewhere safe and never in git.
4. Note the **Key ID** and your **Team ID** (top right of the portal).
5. In Firebase, go to **Project settings** → **Cloud Messaging** → **Apple app configuration** → **APNs Authentication Key** → **Upload**.
6. Choose the `.p8` file, then enter the Key ID and Team ID.

The Worker already sends iPhones a discreet alert (`Tuulit · Take a quick moment for yourself.`). The app's notification extension shows the full text only if the person has turned off "Discreet notifications". Android messages are unchanged.

## 3. App Check for iOS

1. In Firebase, go to **App Check** → **Apps** → **Tuulit iOS** → **App Attest** → **Save**.
2. Debug builds use the App Check debug provider. The first time a debug build runs, Xcode's console prints a debug token. Add it under **App Check** → **Tuulit iOS** → ⋮ → **Manage debug tokens**.
3. The Worker runs `APP_CHECK_MODE = "monitor"`. Leave it there until both apps have run with App Check for a while.

## 4. Build settings (no secrets)

Create `ios/Config/Local.xcconfig`. It's gitignored.

```
TUULIT_API_BASE_URL = https:/$()/tuulit-api.mybeatstar.workers.dev
TUULIT_TEAM_ID = YOURTEAMID
```

The `/$()/` is how xcconfig files write `//`. The Worker URL is public; every real secret stays in the Worker. These are AI, Brevo, the R2 signing key and the service account.

## 5. Build on a Mac

```
brew install xcodegen
cd ios && xcodegen generate && open Tuulit.xcodeproj
```

Then choose the **Tuulit** scheme, pick an iPhone, and **Run**.

## Checking that Android and iPhone share data

1. Sign in with the same account on both phones.
2. Log a mood on the iPhone, then bring the Android app back to the foreground. Android now pulls when it returns to the foreground, and the mood appears.
3. Log a period on Android, then open the iPhone app. The cycle ring and the calendar show it.
4. Change the cycle length or Banana Mode on one phone. Within a few seconds of the other phone coming to the foreground, it shows the same value.
