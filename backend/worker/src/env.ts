/** Bindings and settings for the Tuulit Worker. Secrets are set with `wrangler secret put`, never committed. */
export interface Env {
  // Bindings
  MEDIA: R2Bucket; // private R2 bucket for voice-note backups
  RATE: KVNamespace; // per-user daily counters

  // Plain settings (wrangler.toml [vars])
  FIREBASE_PROJECT_ID: string;
  FIREBASE_PROJECT_NUMBER: string;
  APP_CHECK_MODE: "off" | "monitor" | "enforce";
  AI_PROVIDER: "anthropic" | "openai_compatible";
  AI_MODEL: string;
  AI_BASE_URL?: string; // only for openai_compatible
  AI_DAILY_LIMIT: string;
  BREVO_SENDER_EMAIL: string;
  BREVO_SENDER_NAME: string;
  APP_NAME: string;
  PUBLIC_BASE_URL: string; // this Worker's public URL, used to build signed media URLs

  // Secrets
  AI_API_KEY: string;
  BREVO_API_KEY: string;
  FIREBASE_SERVICE_ACCOUNT: string; // full service-account JSON
  MEDIA_SIGNING_KEY: string; // random 32+ byte string for signing media URLs
}

export class HttpError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

export const json = (body: unknown, status = 200): Response =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" },
  });
