import { Env, HttpError } from "./env";
import { layout, sendEmail } from "./email";

/* Website contact form (https://tuulit.app/contact). Public, so it is protected by an origin
   allow-list, a hidden honeypot field, a minimum fill time and a per-IP daily limit. Messages are
   forwarded to the support inbox through Brevo; the Brevo key never leaves the Worker. Nothing is
   stored except the short-lived rate-limit counter. */

export const CONTACT_TOPICS = ["General enquiry", "Technical support", "Privacy enquiry", "Data deletion", "Feedback"] as const;
const ALLOWED_ORIGINS = [/^https:\/\/tuulit\.app$/, /^https:\/\/www\.tuulit\.app$/, /^https:\/\/[a-z0-9-]+\.tuulit-website\.pages\.dev$/, /^https:\/\/tuulit-website\.pages\.dev$/, /^http:\/\/localhost:\d+$/];
const EMAIL = /^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$/;

export interface ContactInput { name: string; email: string; topic: string; message: string; website?: string; startedAt?: number }

function cors(origin: string | null): Record<string, string> {
  if (!origin || !ALLOWED_ORIGINS.some((r) => r.test(origin))) return {};
  return { "access-control-allow-origin": origin, "access-control-allow-methods": "POST, OPTIONS", "access-control-allow-headers": "content-type", "access-control-max-age": "86400", vary: "Origin" };
}

function reply(body: unknown, status: number, origin: string | null): Response {
  return new Response(JSON.stringify(body), { status, headers: { "content-type": "application/json; charset=utf-8", ...cors(origin) } });
}

const escape = (s: string) => s.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]!);

/** Validates a submission. Returns cleaned values or throws HttpError with a message to show. */
export function validateContact(raw: unknown, now = Date.now()): ContactInput & { spam: boolean } {
  const b = (raw ?? {}) as Record<string, unknown>;
  const name = String(b.name ?? "").trim().slice(0, 80);
  const email = String(b.email ?? "").trim().slice(0, 254);
  const topic = String(b.topic ?? "");
  const message = String(b.message ?? "").trim();
  const startedAt = Number(b.startedAt ?? 0);
  // Honeypot filled in, or submitted faster than a person could type: accept silently, send nothing.
  const spam = String(b.website ?? "") !== "" || (startedAt > 0 && now - startedAt < 3000);
  if (!name) throw new HttpError(400, "Please tell us your name.");
  if (!EMAIL.test(email)) throw new HttpError(400, "Please enter a valid email address so we can reply.");
  if (!(CONTACT_TOPICS as readonly string[]).includes(topic)) throw new HttpError(400, "Please choose a topic.");
  if (message.length < 10) throw new HttpError(400, "Please write a little more (at least 10 characters).");
  if (message.length > 4000) throw new HttpError(400, "Please keep your message under 4,000 characters.");
  return { name, email, topic, message, spam };
}

export async function contact(req: Request, env: Env): Promise<Response> {
  const origin = req.headers.get("origin");
  if (req.method === "OPTIONS") return new Response(null, { status: 204, headers: cors(origin) });
  try {
    if (req.method !== "POST") throw new HttpError(405, "Method not allowed.");
    if (!origin || !ALLOWED_ORIGINS.some((r) => r.test(origin))) throw new HttpError(403, "Not allowed.");
    if (!env.CONTACT_TO) throw new HttpError(503, `The contact form isn't set up yet. Please email us instead.`);
    if (Number(req.headers.get("content-length") ?? 0) > 16_000) throw new HttpError(413, "That message is too long.");
    let body: unknown;
    try { body = await req.json(); } catch { throw new HttpError(400, "Expected JSON."); }
    const c = validateContact(body);
    if (c.spam) return reply({ ok: true }, 200, origin);

    const ip = req.headers.get("cf-connecting-ip") ?? "unknown";
    const key = `contact:${ip}:${new Date().toISOString().slice(0, 10)}`;
    const n = Number((await env.RATE.get(key)) ?? 0);
    if (n >= 5) throw new HttpError(429, "You've sent several messages today. Please try again tomorrow or email us directly.");
    await env.RATE.put(key, String(n + 1), { expirationTtl: 60 * 60 * 26 });

    const html = layout(`Website message: ${escape(c.topic)}`,
      `<p><strong>From:</strong> ${escape(c.name)} &lt;${escape(c.email)}&gt;</p><p><strong>Topic:</strong> ${escape(c.topic)}</p>` +
      `<p style="white-space:pre-wrap">${escape(c.message)}</p><p style="color:#7D6857">Reply to this email to answer ${escape(c.name)} directly.</p>`);
    await sendEmail(env, env.CONTACT_TO, { subject: `Tuulit website: ${c.topic}`, html }, { email: c.email, name: c.name });
    return reply({ ok: true }, 200, origin);
  } catch (e) {
    if (e instanceof HttpError) return reply({ error: e.message }, e.status, origin);
    console.error(JSON.stringify({ event: "contact_error", message: (e as Error).message }));
    return reply({ error: "Something went wrong on our side. Please email us instead." }, 500, origin);
  }
}
