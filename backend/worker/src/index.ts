import { answer, sanitize } from "./ai";
import { contact } from "./contact";
import { removeAllConnections } from "./connect";
import { notify } from "./connect";
import { sendEmail, templates } from "./email";
import { Env, HttpError, json } from "./env";
import { deleteAuthUser, deleteDocByName, listDocs, verificationLink } from "./google";
import { Claims, verifyAppCheck, verifyIdToken } from "./jwt";
import { blob, deleteAllFor, deleteObject, downloadUrl, uploadUrl } from "./media";

/**
 * Tuulit API. Every /v1 route except the signed media blob requires a Firebase ID token.
 * Runs on Cloudflare Workers' free plan alongside Firebase Spark: no Cloud Functions needed.
 */

async function authenticate(req: Request, env: Env): Promise<Claims> {
  const header = req.headers.get("authorization") ?? "";
  if (!header.startsWith("Bearer ")) throw new HttpError(401, "Sign in to use this.");
  const claims = await verifyIdToken(header.slice(7), env);
  if (env.APP_CHECK_MODE !== "off") {
    const ok = await verifyAppCheck(req.headers.get("x-firebase-appcheck"), env);
    if (!ok && env.APP_CHECK_MODE === "enforce") throw new HttpError(401, "This app build isn't verified.");
    if (!ok) console.warn(JSON.stringify({ event: "appcheck_missing" }));
  }
  return claims;
}

/** Per-user daily limit kept in KV. Keeps AI cost bounded and blocks abuse. */
async function limit(env: Env, uid: string, bucket: string, max: number): Promise<void> {
  const day = new Date().toISOString().slice(0, 10);
  const key = `${bucket}:${uid}:${day}`;
  const n = Number((await env.RATE.get(key)) ?? 0);
  if (n >= max) throw new HttpError(429, "Daily limit reached. Try again tomorrow.");
  await env.RATE.put(key, String(n + 1), { expirationTtl: 60 * 60 * 26 });
}

async function readJson(req: Request): Promise<Record<string, unknown>> {
  if (Number(req.headers.get("content-length") ?? 0) > 64_000) throw new HttpError(413, "Request too large.");
  try { return (await req.json()) as Record<string, unknown>; } catch { throw new HttpError(400, "Expected JSON."); }
}

export async function route(req: Request, env: Env): Promise<Response> {
  const url = new URL(req.url);
  const path = url.pathname;
  if (path === "/health") return json({ ok: true });
  if (path === "/v1/media/blob") return blob(env, req);
  if (path === "/v1/contact") return contact(req, env);
  if (req.method !== "POST") throw new HttpError(405, "Method not allowed.");

  const claims = await authenticate(req, env);
  const uid = claims.sub;
  const body = await readJson(req);

  switch (path) {
    case "/v1/ai": {
      await limit(env, uid, "ai", Number(env.AI_DAILY_LIMIT || 60));
      const res = await answer(env, sanitize(body));
      console.log(JSON.stringify({ event: "ai", tool: body.tool, urgency: res.urgency, withContext: !!body.context }));
      return json(res);
    }
    case "/v1/email/welcome": {
      if (!claims.email) throw new HttpError(400, "No email on this account.");
      await limit(env, uid, "welcome", 1);
      await sendEmail(env, claims.email, templates.welcome(String(body.displayName ?? "").slice(0, 40)));
      return json({ ok: true });
    }
    case "/v1/email/verification": {
      if (!claims.email) throw new HttpError(400, "No email on this account.");
      if (claims.email_verified) return json({ ok: true, alreadyVerified: true });
      await limit(env, uid, "verify", 5);
      await sendEmail(env, claims.email, templates.verify(await verificationLink(env, claims.email)));
      return json({ ok: true });
    }
    case "/v1/media/upload-url":
      await limit(env, uid, "upload", 50);
      return uploadUrl(env, uid, body as { kind?: string; contentType?: string; bytes?: number });
    case "/v1/media/download-url":
      return downloadUrl(env, uid, String(body.key ?? ""));
    case "/v1/media/delete":
      return deleteObject(env, uid, String(body.key ?? ""));
    case "/v1/connect/notify":
      await limit(env, uid, "notify", 30);
      return notify(env, uid, body as { connectionId?: string; type?: string; message?: string });
    case "/v1/account/delete":
      return deleteAccount(env, uid, claims);
    default:
      throw new HttpError(404, "Not found.");
  }
}

async function deleteAccount(env: Env, uid: string, claims: Claims): Promise<Response> {
  // Require a fresh sign-in (the app re-authenticates before calling this).
  const authTime = Number(claims.auth_time ?? 0);
  if (Date.now() / 1000 - authTime > 10 * 60) throw new HttpError(401, "For your security, please enter your password again.");
  const userRoot = `users/${uid}`;
  for (const sub of ["cycles", "dailyLogs", "painLogs", "comfort", "games", "achievements", "devices"]) {
    for (const d of await listDocs(env, `${userRoot}/${sub}`)) await deleteDocByName(env, d.name);
  }
  await deleteDocByName(env, `projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents/${userRoot}`);
  await removeAllConnections(env, uid);
  const objects = await deleteAllFor(env, uid);
  await deleteAuthUser(env, uid);
  if (claims.email) await sendEmail(env, claims.email, templates.deleted()).catch(() => undefined);
  console.log(JSON.stringify({ event: "account_deleted", objects }));
  return json({ ok: true });
}

export default {
  async fetch(req: Request, env: Env): Promise<Response> {
    try {
      return await route(req, env);
    } catch (e) {
      if (e instanceof HttpError) return json({ error: e.message }, e.status);
      console.error(JSON.stringify({ event: "unhandled", message: (e as Error).message }));
      return json({ error: "Something went wrong on our side. Please try again." }, 500);
    }
  },
};
