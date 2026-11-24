# Deploying the website

**Target:** Cloudflare Pages project `tuulit-website`, custom domain `tuulit.app`.

## Build

```
cd website
npm ci
npm run build        # outputs website/dist (static HTML, CSS, fonts, images)
npm run preview      # optional local check at http://localhost:4321
```

Node 20 or later. There are no environment variables: the site has no secrets. Public settings (owner details, the contact endpoint and store links) live in `website/src/data/site.ts`.

## Option A: Git integration (recommended)

1. In Cloudflare, go to **Workers & Pages** → **Create** → **Pages** → **Connect to Git** → `Andrew123s/Tuulit`.
2. Enter these settings:

| Setting | Value |
|---|---|
| Production branch | `main` |
| Root directory | `website` |
| Build command | `npm run build` |
| Output directory | `dist` |
| Environment variable | `NODE_VERSION = 20` |

3. Every push to `main` deploys, and pull requests get preview URLs (`*.tuulit-website.pages.dev`). The contact form already accepts these preview URLs.

## Option B: direct upload with Wrangler

```
cd website && npm run build
npx wrangler pages project create tuulit-website --production-branch main   # first time only
npx wrangler pages deploy dist --project-name tuulit-website
```

## After the first deployment

1. Connect the domain: see `NAMECHEAP_DNS_SETUP.md`.
2. In Play Console, set:
   - the privacy policy URL to `https://tuulit.app/privacy`;
   - the account deletion URL to `https://tuulit.app/account-deletion`.
3. When the Play listing is live, set `googlePlayUrl` in `website/src/data/site.ts` and redeploy.

## What `public/_headers` and `public/_redirects` do

- **`_headers`:** HSTS, `nosniff`, a strict referrer policy, a permissions policy, `X-Frame-Options: DENY`, and a Content Security Policy that only allows the site itself plus the contact endpoint. It also sets long-lived caching for fonts and hashed assets.
- **`_redirects`:** permanent redirects from friendly aliases (`/privacy-policy`, `/tos`, `/delete-account`, `/support` …) to the stable URLs.

## Contact form backend

The form posts to the Tuulit Worker endpoint `/v1/contact` (`backend/worker/src/contact.ts`). The Worker setting is `CONTACT_TO` in `wrangler.toml`, which is not a secret. Brevo uses the existing `BREVO_API_KEY` secret. The endpoint was deployed on 2026-11-24.
