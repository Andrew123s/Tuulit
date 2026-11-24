// Static site for https://tuulit.app, deployed to Cloudflare Pages (see docs/website/DEPLOYMENT.md).
import { defineConfig } from "astro/config";

export default defineConfig({
  site: "https://tuulit.app",
  output: "static",
  trailingSlash: "never",
  build: { format: "file" },
  compressHTML: true,
});
