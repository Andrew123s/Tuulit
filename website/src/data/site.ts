/**
 * Single place for facts about the owner and the product's public listing. Update here and every
 * page (footer, legal pages, contact) follows. Values marked "owner to confirm" are tracked in
 * docs/website/PRIVACY_POLICY_DATA_AUDIT.md.
 */
export const site = {
  name: "Tuulit",
  url: "https://tuulit.app",
  tagline: "Menstrual health and wellness companion",
  description:
    "Tuulit is a menstrual health and wellness app: track your cycle, mood and symptoms, find comfort and care, play gentle games, ask questions and share only what you choose.",

  // Owner (provided by the owner on 2026-11-24).
  company: "Ycomspace",
  country: "Germany",
  email: "ycomspacegh@gmail.com",

  // Contact form endpoint on the Tuulit Cloudflare Worker (keeps the Brevo key server-side).
  contactEndpoint: "https://tuulit-api.mybeatstar.workers.dev/v1/contact",

  // Store listings. Leave null until the listing is live: the site then shows "Coming soon"
  // instead of a link, so there is never a dead download button.
  googlePlayUrl: null as string | null,
  appStoreUrl: null as string | null,

  // Legal document versions.
  privacy: { version: "1.0", effective: "24 November 2026", updated: "24 November 2026" },
  terms: { version: "1.0", effective: "24 November 2026", updated: "24 November 2026" },
};
