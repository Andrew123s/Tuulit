# Tuulit UI redesign (2.1.0)

## Why

Seven testers said the 2.0 build looked basic, wasn't feminine enough, felt depressing and didn't excite them. The screen recording (`Screen_Recording_20261123_175559_Tuulit.mp4`) explains most of it.

## Audit of the 2.0 recording

| Area | What the recording shows | Effect on testers |
|---|---|---|
| Theme | The app followed the system theme, and the test phone was in dark mode. Every screen rendered on near-black. | The main cause of "depressing". |
| Selected states | Chips, segmented controls, quiz answers, mood buttons and need tiles all turned solid charcoal when selected. | Heavy and serious rather than playful. |
| Home | A thin horizontal ribbon, plain text and stacked panels. There was no focal visual. | Basic, with nothing to look forward to. |
| Calendar | Square, flat day cells. | Spreadsheet feel. |
| Care | A list of rows with grey icons. | Felt like a settings page. |
| Play | Every game had the same plain card, and the game screens had no identity. | Nothing exciting. |
| Ask | An empty chat field with no guidance. | It wasn't clear what to ask. |
| Navigation | A flat tab bar with a black Log button. | Utilitarian. |
| Settings | Grey icon rows. | Cold. |

## New direction

The new look is light-first, warm, feminine, playful and premium. Personality comes from flat shapes and colour, not from gradients, neon or purple "AI" styling. There are no emoji illustrations.

### Palette (light)

| Token | Hex | Use |
|---|---|---|
| cream / oat | `#FFF8F0` / `#FFFDF9` | page and cards |
| sand / tan | `#FBEEDD` / `#F2D9BE` | quiet surfaces |
| banana / bananaSoft | `#F7C948` / `#FFF0C2` | primary energy, Log button, today marker |
| period (coral) / periodSoft / periodDeep | `#E2725B` / `#FDDDD3` / `#B5482F` | period, progress, PIN |
| peach / peachSoft | `#FFC9AE` / `#FFE7DA` | selection, follicular phase |
| rose / roseSoft | `#E7A6B0` / `#F9E1E4` | luteal phase, comfort |
| fertile (muted lilac) / fertileSoft / lilacDeep | `#9C88C9` / `#ECE5F7` / `#6F5AA8` | ovulation, symptoms, Ask |
| toffee (cocoa) | `#9A5B3C` | accents and overlines |
| ink (charcoal) | `#2B2320` | text and primary buttons |

Warm dark is still available under You → Appearance. **The default is now Light**, not System.

## Illustration system

`ui/designsystem/Illustrations.kt` holds every illustration. They are all flat Canvas drawings with no image assets.

- `Blob` draws organic background shapes behind heroes.
- `CycleRing` shows one arc per day, coloured by phase, with a banana knob on today.
- `MoodBlob` gives five mood faces, each on its own soft colour.
- `Illustration(Illo.*)` provides spot art for care sessions: heat, breath, lotus, moon, heart, sun, cup, spark.
- `GameArt` gives each of the eight games its own artwork and colour (`gameLook`).
- The banana mascot now has blush cheeks. It is used on the heroes for Home, Care, Ask, You and Welcome, and as the Ask chat avatar.

## Screen by screen

- **Home:**
  - time-of-day greeting
  - phase-tinted hero with the cycle ring and countdown
  - "My period started" button
  - four quick actions
  - mood check-in
  - phase recommendation
  - "What would help right now?" card
- **Cycle:**
  - phase summary card with three stats
  - round calendar days (coral period, lilac ovulation, banana ring on today)
  - pill legend
  - day detail card
- **Log:**
  - coral flow steps
  - mood blobs
  - lilac symptom tiles of equal height
  - banana bars for energy
  - banana Done button
- **Care:**
  - coloured hero
  - featured "Made for today" session
  - category chips
  - coloured session carousel
  - Comfort Library preview
  - collapsible doctor guidance
- **Play:**
  - featured Banana Catch banner
  - today's pick, chosen from the date
  - real progress (games played, moments unlocked)
  - illustrated game grid
  - coloured quiz cards
  - every game screen gets a header tinted by `gameLook`
  - Banana Catch has drawn bananas, pebbles and a basket
- **Ask:**
  - lilac hero with the mascot
  - six shortcuts: Explain my cycle, Why am I feeling this way?, Ask about a symptom, Prepare for my period, Help me feel better, Prepare for a doctor appointment
  - mascot avatar on replies
  - peach user bubbles
  - animated typing dots
- **Navigation:**
  - floating pill tab bar with a peach active state
  - 66 dp banana Log button that rotates on press
- **You and settings:**
  - rose profile card with the mascot
  - settings grouped on an oat panel
  - every list icon on a soft colour tile (applies app-wide through `ListRow`)
- **Other screens:**
  - Insights bars in peach and coral
  - coral pain scale and PIN dots
  - illustrated Connect header
  - Welcome screen with blobs behind a happy mascot
  - peach need tiles
  - lilac quiz answers

## What did not change

Navigation routes, data, sync, AI, Connect and safety logic are all unchanged. No placeholder buttons or invented data were added. The progress numbers on Play are read from the local database.
