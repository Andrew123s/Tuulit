# Website testing

Tested on 2026-11-24 against the production build (`npm run build` + `npm run preview`) in the browser pane.

| Check | Result |
|---|---|
| Build | 6 HTML pages plus `sitemap.xml`; `dist` is 567 KB including fonts and screenshots |
| Internal links and anchors (script over every page in `dist`) | No broken links, images or `#anchors` |
| Titles and meta descriptions | Unique on every page; canonical, Open Graph and Twitter tags present; homepage JSON-LD (`MobileApplication`) |
| Responsive (375 mobile, 768 tablet, 1280 desktop) | No horizontal overflow on `/` or `/privacy`; legal tables scroll inside their container on small screens |
| Mobile menu | Opens, sets `aria-expanded`, closes after a link tap |
| Contact form, client side | Empty or invalid fields are blocked, the first bad field is focused and marked `aria-invalid`, and the message is announced |
| Contact endpoint, live Worker | `OPTIONS` from tuulit.app → 204 with CORS; other origin → 403; invalid email → 400 with a readable message |
| Contact endpoint, unit tests | Validation, honeypot and timing spam rules, CORS (Worker `npm test`: 12 passed) |
| Third-party requests and cookies | None: fonts and images are self-hosted |
| Reduced motion | Reveal and mascot animation disabled under `prefers-reduced-motion` |
| Store links | No live listing yet, so "Coming soon to Google Play" is shown as non-link text, never a dead link |

## Not yet tested (needs deployment or the owner)

- **A real contact-form submission end to end.** It sends an email to the support inbox. Do it once the site is live and Brevo domain authentication is finished.
- **HTTPS, HSTS, `_headers` and `_redirects`.** Cloudflare Pages applies these. Check after the first deployment, for example with `curl -I https://tuulit.app/privacy-policy`.
- **Lighthouse or PageSpeed** on the live URL.
- **Screen-reader passes** with TalkBack and VoiceOver on real phones.
