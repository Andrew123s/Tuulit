import { Env, HttpError } from "./env";

/* JWT helpers built on WebCrypto. Verifies Firebase ID tokens and App Check tokens, and signs
   service-account assertions for Google APIs. No third-party crypto libraries. */

const b64url = {
  decode(s: string): Uint8Array {
    const pad = s.length % 4 === 0 ? "" : "=".repeat(4 - (s.length % 4));
    const bin = atob(s.replace(/-/g, "+").replace(/_/g, "/") + pad);
    return Uint8Array.from(bin, (c) => c.charCodeAt(0));
  },
  encode(data: ArrayBuffer | Uint8Array | string): string {
    const bytes = typeof data === "string" ? new TextEncoder().encode(data) : new Uint8Array(data);
    let bin = "";
    bytes.forEach((b) => (bin += String.fromCharCode(b)));
    return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  },
};
export { b64url };

type Jwk = JsonWebKey & { kid: string };
const jwksCache = new Map<string, { keys: Jwk[]; until: number }>();

async function jwks(url: string): Promise<Jwk[]> {
  const hit = jwksCache.get(url);
  if (hit && hit.until > Date.now()) return hit.keys;
  const res = await fetch(url);
  if (!res.ok) throw new HttpError(503, "Couldn't load signing keys.");
  const body = (await res.json()) as { keys: Jwk[] };
  const maxAge = Number(/max-age=(\d+)/.exec(res.headers.get("cache-control") ?? "")?.[1] ?? 3600);
  jwksCache.set(url, { keys: body.keys, until: Date.now() + maxAge * 1000 });
  return body.keys;
}

export interface Claims {
  sub: string;
  aud: string | string[];
  iss: string;
  exp: number;
  iat: number;
  email?: string;
  email_verified?: boolean;
  name?: string;
  [k: string]: unknown;
}

async function verifyRs256(token: string, jwksUrl: string): Promise<Claims> {
  const parts = token.split(".");
  if (parts.length !== 3) throw new HttpError(401, "Malformed token.");
  const header = JSON.parse(new TextDecoder().decode(b64url.decode(parts[0])));
  if (header.alg !== "RS256") throw new HttpError(401, "Unexpected token algorithm.");
  const key = (await jwks(jwksUrl)).find((k) => k.kid === header.kid);
  if (!key) throw new HttpError(401, "Unknown signing key.");
  const cryptoKey = await crypto.subtle.importKey("jwk", key, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["verify"]);
  const ok = await crypto.subtle.verify("RSASSA-PKCS1-v1_5", cryptoKey, b64url.decode(parts[2]), new TextEncoder().encode(`${parts[0]}.${parts[1]}`));
  if (!ok) throw new HttpError(401, "Invalid token signature.");
  const claims = JSON.parse(new TextDecoder().decode(b64url.decode(parts[1]))) as Claims;
  const now = Math.floor(Date.now() / 1000);
  if (claims.exp < now - 30) throw new HttpError(401, "Token expired.");
  if (claims.iat > now + 300) throw new HttpError(401, "Token issued in the future.");
  return claims;
}

/** Verifies a Firebase Auth ID token and returns its claims. */
export async function verifyIdToken(token: string, env: Env): Promise<Claims> {
  const claims = await verifyRs256(token, "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com");
  if (claims.aud !== env.FIREBASE_PROJECT_ID) throw new HttpError(401, "Token is for another project.");
  if (claims.iss !== `https://securetoken.google.com/${env.FIREBASE_PROJECT_ID}`) throw new HttpError(401, "Wrong token issuer.");
  if (!claims.sub) throw new HttpError(401, "Token has no subject.");
  return claims;
}

/** Verifies an App Check token. Returns false instead of throwing so "monitor" mode can log. */
export async function verifyAppCheck(token: string | null, env: Env): Promise<boolean> {
  if (!token) return false;
  try {
    const c = await verifyRs256(token, "https://firebaseappcheck.googleapis.com/v1/jwks");
    const aud = Array.isArray(c.aud) ? c.aud : [c.aud];
    return aud.includes(`projects/${env.FIREBASE_PROJECT_NUMBER}`) && c.iss === `https://firebaseappcheck.googleapis.com/${env.FIREBASE_PROJECT_NUMBER}`;
  } catch {
    return false;
  }
}

/* ---- Google service-account access tokens (Firestore REST, FCM v1, Identity Toolkit) ---- */

interface ServiceAccount { client_email: string; private_key: string; project_id: string }
let cachedToken: { token: string; until: number } | null = null;

function pemToPkcs8(pem: string): ArrayBuffer {
  const body = pem.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
  return Uint8Array.from(atob(body), (c) => c.charCodeAt(0)).buffer;
}

export async function googleAccessToken(env: Env): Promise<string> {
  if (cachedToken && cachedToken.until > Date.now() + 60_000) return cachedToken.token;
  if (!env.FIREBASE_SERVICE_ACCOUNT) throw new HttpError(503, "Server is missing FIREBASE_SERVICE_ACCOUNT.");
  const sa = JSON.parse(env.FIREBASE_SERVICE_ACCOUNT) as ServiceAccount;
  const now = Math.floor(Date.now() / 1000);
  const header = b64url.encode(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claims = b64url.encode(JSON.stringify({
    iss: sa.client_email,
    scope: [
      "https://www.googleapis.com/auth/datastore",
      "https://www.googleapis.com/auth/firebase.messaging",
      "https://www.googleapis.com/auth/identitytoolkit",
    ].join(" "),
    aud: "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  }));
  const key = await crypto.subtle.importKey("pkcs8", pemToPkcs8(sa.private_key), { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const sig = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(`${header}.${claims}`));
  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: `${header}.${claims}.${b64url.encode(sig)}` }),
  });
  if (!res.ok) throw new HttpError(503, "Couldn't authenticate to Google APIs.");
  const body = (await res.json()) as { access_token: string; expires_in: number };
  cachedToken = { token: body.access_token, until: Date.now() + body.expires_in * 1000 };
  return body.access_token;
}

/* ---- HMAC signing for media URLs ---- */

export async function hmac(key: string, data: string): Promise<string> {
  const k = await crypto.subtle.importKey("raw", new TextEncoder().encode(key), { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
  return b64url.encode(await crypto.subtle.sign("HMAC", k, new TextEncoder().encode(data)));
}

export function timingSafeEqual(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}
