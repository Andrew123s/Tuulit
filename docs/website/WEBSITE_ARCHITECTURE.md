# Website architecture

The official site is **https://tuulit.app**. Its source is in `website/`.

| Choice | Why |
|---|---|
| **Astro 5, static output, TypeScript** | No web code existed before. Astro ships zero JavaScript by default. Pages are plain HTML and CSS, apart from about 1 KB for the mobile menu and reveal effects, and the contact form script. That beats a Next.js static export on page weight, with the same SEO-friendly routing. |
| **Cloudflare Pages** | Free, global HTTPS, and supports `_headers` and `_redirects`. It's in the same account as the Tuulit Worker. |
| **No database, no auth, no Firebase** | The site is informational. The only dynamic piece is the contact form. |
| **Contact form → existing Worker** | `POST https://tuulit-api.mybeatstar.workers.dev/v1/contact`. It's protected by an origin allow-list, a honeypot field, a minimum fill time, and a limit of 5 messages per IP per day (KV). Messages are forwarded by Brevo to `CONTACT_TO`. The Brevo key stays in the Worker. |

## Structure

```
website/
  astro.config.mjs        site URL, static output, clean URLs (no trailing slash)
  src/data/site.ts        owner details, contact endpoint, store links, legal versions (edit here)
  src/layouts/Base.astro  <head> metadata (title, description, canonical, Open Graph, JSON-LD), header, footer
  src/components/LegalLayout.astro   shared layout for legal pages (contents list, version chips)
  src/styles/global.css   design tokens and shared styles
  src/pages/
    index.astro           homepage
    privacy.astro         /privacy
    terms.astro           /terms
    account-deletion.astro /account-deletion
    contact.astro         /contact
    404.astro
    sitemap.xml.ts        /sitemap.xml
  public/
    fonts/                Fraunces and Plus Jakarta Sans (subset woff2, from the app)
    screens/              real Android screenshots (WebP)
    mascot.svg            banana mascot, the app's vector paths
    og.png                1200×630 social image
    robots.txt  _headers  _redirects
```

## Pages

| URL | Purpose |
|---|---|
| `/` | Brand and product: hero, why Tuulit, five feature stories with real screens, Insights and Connect, privacy band, philosophy |
| `/privacy` | Privacy Policy (Play requirement), versioned |
| `/terms` | Terms of Service, versioned |
| `/account-deletion` | In-app steps, email request, what is deleted (Play requirement) |
| `/contact` | Support topics and a working form |

**Store buttons:** while `googlePlayUrl` is `null`, every store button reads "Coming soon to Google Play" and isn't a link, so there are no dead links. Set the URL in `site.ts` when the listing goes live.
