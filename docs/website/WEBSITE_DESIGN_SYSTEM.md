# Website design system

The website uses the same tokens as the Android app (`ui/designsystem/Theme.kt`). Change both together.

## Colour (CSS variables in `src/styles/global.css`)

| Token | Hex | Use |
|---|---|---|
| `--cream` | #FFF8F0 | Page background (warm ivory) |
| `--oat` | #FFFDF9 | Raised panels, form |
| `--sand` | #FBEEDD | Chips, quiet buttons, fields |
| `--ink` | #2B2320 | Text, primary buttons, privacy band, footer |
| `--ink-muted` | #6B5A50 | Secondary text |
| `--toffee` | #9A5B3C | Overlines, accents (cocoa) |
| `--banana` / `--banana-soft` | #F7C948 / #FFF0C2 | Main call to action, highlights |
| `--coral` / `--coral-soft` / `--coral-deep` | #E2725B / #FDDDD3 / #B5482F | Period colour, links, "Your Tuulit." |
| `--peach-soft` | #FFE7DA | Hero blob, nav hover |
| `--rose-soft` | #F9E1E4 | Insights card, health callouts |
| `--lilac-soft` | #ECE5F7 | Symptoms and Ask features |

The palette avoids neon, large areas of pink and gradients. The one dark surface, the privacy band, is deliberate: it gives the privacy message weight.

## Type

| Role | Font | Notes |
|---|---|---|
| Headings | **Fraunces** (variable, optical size 24–96) | Self-hosted woff2, Latin subset, 64 KB |
| Body | **Plus Jakarta Sans** (variable) | 31 KB |

Sizes are fluid (`clamp()`): h1 2.6–5 rem, h2 2–3.2 rem, body 17 px. Overlines are uppercase, 0.78 rem, with 0.12 em tracking.

## Shapes and imagery

- **Corners:** 999 px pills for buttons and chips; 20–40 px corners for content blocks. Cards are used sparingly: feature stories are colour fields, not a grid of identical boxes.
- **Phone mockups:** CSS-only frame (`.phone`) around **real app screenshots** in `public/screens`. Never mock up features that don't exist.
- **Mascot:** `public/mascot.svg` is drawn from the app's exact vector paths. It appears in the logo, the hero (gentle bob), the privacy band and the 404 page.
- **Blobs:** soft SVG shapes behind the hero, as in the app's illustration system.

## Motion

- A gentle reveal on scroll, a hover lift on buttons and the mascot bob. Nothing else moves.
- All motion stops with `prefers-reduced-motion`.

## Accessibility

- Skip link and visible focus rings.
- Text meets WCAG AA contrast on its backgrounds.
- Labelled form fields, with errors announced through `aria-live`.
- A menu button with `aria-expanded`; landmarks and descriptive alt text.
- Minimum touch targets of 44 px.
