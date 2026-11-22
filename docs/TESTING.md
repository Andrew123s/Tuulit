# Testing

## How to run

| Suite | Command | Needs |
|---|---|---|
| Android unit, Room, API client and Compose UI tests | `./gradlew :app:testDebugUnitTest` | JDK 17. Robolectric runs on SDK 35 (`app/src/test/resources/robolectric.properties`). |
| Worker unit tests and typecheck | `cd backend/worker && npm test && npm run typecheck` | Node 20+ |
| Worker bundle check | `cd backend/worker && npx wrangler deploy --dry-run --outdir .wrangler/dry` | Node 20+ |
| Firestore rules (emulator) | `cd firebase && npm test` | Node 20+, Java 11+ |
| Debug APK | `./gradlew :app:assembleDebug` | Android SDK 36 |

## What is covered

**Android: 44 tests, all passing (2026-11-22)**
- `CycleCalculatorTest` (6): predictions, irregular ranges, fertile window, Mood Map, pain patterns.
- `SafetyGuardTest` (4): emergency, urgent and benign phrases, with no false positive on "101".
- `ValidationTest` (5): email, password, PIN, invite code and range rules; PIN hashing is salted and never stores plaintext.
- `TodayAndSuggestionTest` (6): Today hero states (no data, countdown, during period), suggestions, Care hero selection, content integrity.
- `RemindersTest` (4): quiet hours across midnight, next-run scheduling, period heads-up timing.
- `TrackingRepositoryTest` (12, Room in memory):
  - cycle creation, extension and earlier start
  - spotting doesn't start a cycle
  - removing flow removes the cycle
  - duplicate prevention
  - tombstones
  - edit validation
  - onboarding period with no invented flow
  - pain log clamp, delete and undo
  - CSV escaping and formula neutralising
  - Care library crediting and achievements
  - best scores only go up
- `ApiClientTest` (3, MockWebServer): Bearer and App Check headers; 401, 429 and 500 mapping; no request without a token or URL.
- `DesignSystemUiTest` (4, Compose on a 360×760 dp screen): buttons and the disabled state, chip and segmented selection, field error text, calendar day selection by its accessibility description.

**Worker: 9 tests, all passing**
- Safety rules
- AI request validation and size limits
- Media key ownership
- Signed-URL round trip, tampering, and that a PUT signature can't be used to GET
- Upload type and size limits
- Health route without auth, rejecting unauthenticated and forged tokens
- Email template escaping, with no health words in any email

**Firestore rules: 7 tests, all passing against the emulator**
- Owner-only backup, collection allow-list and size limits
- Invite creation rules
- One-time acceptance with permissions off
- Expired and self-invites refused
- Only the owner changes permissions; either side disconnects; strangers see nothing
- Shared status limited to permitted fields; member reads; stranger denied

## Manual checks done

See `IMPLEMENTATION_STATUS.md` for what was run on the emulator.

## Not yet automated

- Instrumented end-to-end tests on a device for Auth, sync and Connect. These need a real Firebase project; the Firebase emulator suite can be used once `google-services.json` exists.
- Screenshot tests across screen sizes. Layouts were checked on the emulator, and the design caps content at 600 dp with tablet navigation planned.
- Game feel (Banana Catch speed curve, Calm Tap timing) needs hands-on play-testing.
