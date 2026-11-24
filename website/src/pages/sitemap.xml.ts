import type { APIRoute } from "astro";

// Public, indexable pages. Keep in sync with src/pages.
const pages = [
  { path: "/", priority: "1.0" },
  { path: "/privacy", priority: "0.8" },
  { path: "/terms", priority: "0.6" },
  { path: "/account-deletion", priority: "0.6" },
  { path: "/contact", priority: "0.5" },
];

export const GET: APIRoute = ({ site }) => {
  const base = (site?.toString() ?? "https://tuulit.app/").replace(/\/$/, "");
  const lastmod = new Date().toISOString().slice(0, 10);
  const body = `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${pages
    .map((p) => `  <url><loc>${base}${p.path}</loc><lastmod>${lastmod}</lastmod><priority>${p.priority}</priority></url>`)
    .join("\n")}\n</urlset>\n`;
  return new Response(body, { headers: { "content-type": "application/xml" } });
};
