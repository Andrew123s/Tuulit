import { Env, HttpError, json } from "./env";
import { hmac, timingSafeEqual } from "./jwt";

/* Voice-note storage in a private R2 bucket. The app never holds R2 credentials: the Worker issues
   short-lived URLs to itself, signed with HMAC, and streams bytes to and from the R2 binding.
   Object keys always start with users/{uid}/, so one person can never address another's files. */

const TTL_SECONDS = 300;
const MAX_BYTES = 10 * 1024 * 1024;
const ALLOWED_TYPES = ["audio/mp4", "audio/m4a", "audio/aac"];

async function sign(env: Env, method: string, key: string, exp: number): Promise<string> {
  if (!env.MEDIA_SIGNING_KEY || env.MEDIA_SIGNING_KEY.length < 32) throw new HttpError(503, "Media signing isn't configured on the server.");
  return hmac(env.MEDIA_SIGNING_KEY, `${method}\n${key}\n${exp}`);
}

export async function signedUrl(env: Env, method: "PUT" | "GET", key: string): Promise<string> {
  const exp = Math.floor(Date.now() / 1000) + TTL_SECONDS;
  const sig = await sign(env, method, key, exp);
  return `${env.PUBLIC_BASE_URL.replace(/\/$/, "")}/v1/media/blob?key=${encodeURIComponent(key)}&exp=${exp}&sig=${sig}`;
}

export function ownsKey(uid: string, key: string): boolean {
  return key.startsWith(`users/${uid}/`) && !key.includes("..");
}

export async function uploadUrl(env: Env, uid: string, body: { kind?: string; contentType?: string; bytes?: number }): Promise<Response> {
  if (body.kind !== "voice") throw new HttpError(400, "Only voice notes can be uploaded.");
  if (!ALLOWED_TYPES.includes(body.contentType ?? "")) throw new HttpError(415, "Unsupported audio type.");
  if (!body.bytes || body.bytes <= 0 || body.bytes > MAX_BYTES) throw new HttpError(413, "Recordings must be under 10 MB.");
  const key = `users/${uid}/voice/${crypto.randomUUID()}.m4a`;
  return json({ key, url: await signedUrl(env, "PUT", key), expiresIn: TTL_SECONDS });
}

export async function downloadUrl(env: Env, uid: string, key: string): Promise<Response> {
  if (!ownsKey(uid, key)) throw new HttpError(403, "Not your file.");
  return json({ url: await signedUrl(env, "GET", key), expiresIn: TTL_SECONDS });
}

export async function deleteObject(env: Env, uid: string, key: string): Promise<Response> {
  if (!ownsKey(uid, key)) throw new HttpError(403, "Not your file.");
  await env.MEDIA.delete(key);
  return json({ ok: true });
}

export async function deleteAllFor(env: Env, uid: string): Promise<number> {
  let cursor: string | undefined;
  let n = 0;
  do {
    const page = await env.MEDIA.list({ prefix: `users/${uid}/`, cursor });
    if (page.objects.length) await env.MEDIA.delete(page.objects.map((o) => o.key));
    n += page.objects.length;
    cursor = page.truncated ? page.cursor : undefined;
  } while (cursor);
  return n;
}

/** Handles the signed URL itself: PUT stores, GET streams back. */
export async function blob(env: Env, req: Request): Promise<Response> {
  const u = new URL(req.url);
  const key = u.searchParams.get("key") ?? "";
  const exp = Number(u.searchParams.get("exp") ?? 0);
  const sig = u.searchParams.get("sig") ?? "";
  if (!key.startsWith("users/") || exp < Math.floor(Date.now() / 1000)) throw new HttpError(403, "This link has expired.");
  if (!timingSafeEqual(sig, await sign(env, req.method, key, exp))) throw new HttpError(403, "Invalid link.");
  if (req.method === "PUT") {
    const len = Number(req.headers.get("content-length") ?? 0);
    if (!len || len > MAX_BYTES) throw new HttpError(413, "Recordings must be under 10 MB.");
    const type = req.headers.get("content-type") ?? "";
    if (!ALLOWED_TYPES.some((t) => type.startsWith(t))) throw new HttpError(415, "Unsupported audio type.");
    await env.MEDIA.put(key, req.body, { httpMetadata: { contentType: "audio/mp4" } });
    return json({ ok: true });
  }
  if (req.method === "GET") {
    const obj = await env.MEDIA.get(key);
    if (!obj) throw new HttpError(404, "Not found.");
    return new Response(obj.body, { headers: { "content-type": "audio/mp4", "cache-control": "private, no-store" } });
  }
  throw new HttpError(405, "Method not allowed.");
}
