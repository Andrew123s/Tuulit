import { Env, HttpError, json } from "./env";
import { deleteDocByName, fsGet, getDoc, listDocs, queryEquals, sendFcm } from "./google";

/* Delivers Connect notifications. The Worker reads the connection with admin rights and only
   sends when the caller is part of it and (for SOS) the owner has the rough-day signal on.
   Messages carry no health data beyond what the person chose to send. */

type NotifyType = "sos" | "reply" | "accepted";

export async function notify(env: Env, uid: string, body: { connectionId?: string; type?: string; message?: string }): Promise<Response> {
  const id = String(body.connectionId ?? "");
  const type = body.type as NotifyType;
  if (!/^[A-Za-z0-9]{10,64}_[A-Za-z0-9]{10,64}$/.test(id)) throw new HttpError(400, "Bad connection id.");
  if (!["sos", "reply", "accepted"].includes(type)) throw new HttpError(400, "Bad notification type.");
  const doc = await getDoc(env, `connections/${id}`);
  if (!doc) throw new HttpError(404, "That connection no longer exists.");
  const f = doc.fields ?? {};
  const owner = fsGet(f.ownerUid) as string;
  const member = fsGet(f.memberUid) as string;
  const perms = (fsGet(f.permissions) ?? {}) as Record<string, boolean>;
  let recipient: string;
  let fromName: string;
  if (type === "sos") {
    if (uid !== owner) throw new HttpError(403, "Only the person sharing can send an SOS.");
    if (!perms.roughDay) throw new HttpError(403, "Rough-day signal is off for this person.");
    recipient = member; fromName = fsGet(f.ownerName) as string;
  } else if (type === "reply") {
    if (uid !== member) throw new HttpError(403, "Only the trusted person can reply.");
    recipient = owner; fromName = fsGet(f.memberName) as string;
  } else {
    if (uid !== member) throw new HttpError(403, "Only the invited person can accept.");
    recipient = owner; fromName = fsGet(f.memberName) as string;
  }
  const message = String(body.message ?? "").slice(0, 140);
  const devices = await listDocs(env, `users/${recipient}/devices`);
  let sent = 0;
  for (const d of devices) {
    const tokenDoc = await getDoc(env, d.name.split("/documents/")[1]);
    const token = fsGet(tokenDoc?.fields?.token) as string | undefined;
    if (!token) continue;
    const platform = fsGet(tokenDoc?.fields?.platform) as string | undefined;
    const ok = await sendFcm(env, token, { type, fromName: fromName || "Your person", message, connectionId: id }, platform);
    if (ok) sent++; else await deleteDocByName(env, d.name); // prune dead tokens
  }
  return json({ ok: true, delivered: sent });
}

/** Removes every connection, shared status and invite that involves the user. */
export async function removeAllConnections(env: Env, uid: string): Promise<void> {
  for (const field of ["ownerUid", "memberUid"]) {
    for (const d of await queryEquals(env, "connections", field, uid)) {
      const id = d.name.split("/").pop()!;
      const base = d.name.split("/documents/")[0] + "/documents";
      await deleteDocByName(env, `${base}/shared/${id}`);
      await deleteDocByName(env, d.name);
    }
  }
  for (const d of await queryEquals(env, "invites", "ownerUid", uid)) await deleteDocByName(env, d.name);
}
