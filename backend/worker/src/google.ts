import { Env, HttpError } from "./env";
import { googleAccessToken } from "./jwt";

/* Minimal admin clients for Firestore REST, FCM HTTP v1 and Identity Toolkit, authenticated with
   the Firebase service account. All of these are available on the Spark plan. */

const fsBase = (env: Env) => `https://firestore.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/databases/(default)/documents`;

type FsValue = { stringValue?: string; booleanValue?: boolean; integerValue?: string; nullValue?: null; mapValue?: { fields?: Record<string, FsValue> } };
export interface FsDoc { name: string; fields?: Record<string, FsValue> }

export function fsGet(v: FsValue | undefined): unknown {
  if (!v) return undefined;
  if ("stringValue" in v) return v.stringValue;
  if ("booleanValue" in v) return v.booleanValue;
  if ("integerValue" in v) return Number(v.integerValue);
  if ("mapValue" in v) return Object.fromEntries(Object.entries(v.mapValue?.fields ?? {}).map(([k, x]) => [k, fsGet(x)]));
  return null;
}

async function gfetch(env: Env, url: string, init: RequestInit = {}): Promise<Response> {
  const token = await googleAccessToken(env);
  return fetch(url, { ...init, headers: { ...(init.headers ?? {}), authorization: `Bearer ${token}`, "content-type": "application/json" } });
}

export async function getDoc(env: Env, path: string): Promise<FsDoc | null> {
  const r = await gfetch(env, `${fsBase(env)}/${path}`);
  if (r.status === 404) return null;
  if (!r.ok) throw new HttpError(502, "Database read failed.");
  return (await r.json()) as FsDoc;
}

export async function listDocs(env: Env, collectionPath: string): Promise<FsDoc[]> {
  const out: FsDoc[] = [];
  let pageToken = "";
  do {
    const r = await gfetch(env, `${fsBase(env)}/${collectionPath}?pageSize=300${pageToken ? `&pageToken=${pageToken}` : ""}&mask.fieldPaths=__name__`);
    if (!r.ok) throw new HttpError(502, "Database list failed.");
    const body = (await r.json()) as { documents?: FsDoc[]; nextPageToken?: string };
    out.push(...(body.documents ?? []));
    pageToken = body.nextPageToken ?? "";
  } while (pageToken);
  return out;
}

export async function queryEquals(env: Env, collection: string, field: string, value: string): Promise<FsDoc[]> {
  const r = await gfetch(env, `${fsBase(env)}:runQuery`, {
    method: "POST",
    body: JSON.stringify({ structuredQuery: { from: [{ collectionId: collection }], where: { fieldFilter: { field: { fieldPath: field }, op: "EQUAL", value: { stringValue: value } } } } }),
  });
  if (!r.ok) throw new HttpError(502, "Database query failed.");
  const rows = (await r.json()) as Array<{ document?: FsDoc }>;
  return rows.flatMap((x) => (x.document ? [x.document] : []));
}

export async function deleteDocByName(env: Env, name: string): Promise<void> {
  const r = await gfetch(env, `https://firestore.googleapis.com/v1/${name}`, { method: "DELETE" });
  if (!r.ok && r.status !== 404) throw new HttpError(502, "Database delete failed.");
}

/** Sends a data-only FCM message. Returns false when the token is no longer valid. */
export async function sendFcm(env: Env, token: string, data: Record<string, string>): Promise<boolean> {
  const r = await gfetch(env, `https://fcm.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/messages:send`, {
    method: "POST",
    body: JSON.stringify({ message: { token, data, android: { priority: "HIGH" } } }),
  });
  if (r.ok) return true;
  if (r.status === 404 || r.status === 400) return false;
  throw new HttpError(502, "Push service failed.");
}

/** Generates an email-verification link for the user (sent by Brevo instead of Firebase's default email). */
export async function verificationLink(env: Env, email: string): Promise<string> {
  const r = await gfetch(env, `https://identitytoolkit.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/accounts:sendOobCode`, {
    method: "POST",
    body: JSON.stringify({ requestType: "VERIFY_EMAIL", email, returnOobLink: true }),
  });
  if (!r.ok) throw new HttpError(502, "Couldn't create a verification link.");
  return ((await r.json()) as { oobLink: string }).oobLink;
}

export async function deleteAuthUser(env: Env, uid: string): Promise<void> {
  const r = await gfetch(env, `https://identitytoolkit.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/accounts:delete`, {
    method: "POST",
    body: JSON.stringify({ localId: uid }),
  });
  if (!r.ok) throw new HttpError(502, "Couldn't delete the sign-in account.");
}
