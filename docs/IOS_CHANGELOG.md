# iOS changelog

## 2.1.0 (unreleased): first native iPhone version

- **Native app.** SwiftUI app with the same features as Android 2.1.0. It shares the same Firebase project, Firestore data and Cloudflare Worker.
- **TuulitCore package.** The Android domain logic, local store and sync rules, ported line for line, with tests.
- **Design.** The same palette, fonts, icons, banana mascot and illustrations as Android. The app icon is rendered from Android's launcher artwork.
- **iOS adaptations:**
  - native sheets
  - swipe-back
  - haptics
  - Dynamic Type
  - VoiceOver labels
  - Reduce Motion
  - an app-switcher privacy cover when app lock is on
  - Data Protection for the local store
- **Shared backend changes:**
  - The Worker sends a discreet APNs alert to iPhones.
  - Android now pulls on returning to the foreground.
  - Android now syncs the shared settings.
  - Android now uses fixed starter-kit IDs.
