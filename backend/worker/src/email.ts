import { Env, HttpError, requireSecret } from "./env";

/* Brevo transactional email. The API key exists only as a Worker secret. Templates are inline,
   use the Tuulit palette, and never contain cycle or health details. */

const escape = (s: string) => s.replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]!);

export function layout(title: string, bodyHtml: string, cta?: { label: string; url: string }): string {
  const button = cta
    ? `<p style="margin:28px 0"><a href="${escape(cta.url)}" style="background:#221E1A;color:#FBF7EF;text-decoration:none;font-weight:600;padding:14px 24px;border-radius:999px;display:inline-block">${escape(cta.label)}</a></p>`
    : "";
  return `<!doctype html><html><body style="margin:0;background:#F6F0E4;font-family:'Plus Jakarta Sans',Helvetica,Arial,sans-serif;color:#221E1A">
<table role="presentation" width="100%" cellpadding="0" cellspacing="0"><tr><td align="center" style="padding:32px 16px">
<table role="presentation" width="100%" style="max-width:520px;background:#FBF7EF;border-radius:28px" cellpadding="0" cellspacing="0"><tr><td style="padding:36px 32px">
<p style="margin:0 0 20px;font-size:13px;letter-spacing:.8px;text-transform:uppercase;color:#5E554A;font-weight:600">Tuulit</p>
<h1 style="margin:0 0 12px;font-family:Fraunces,Georgia,serif;font-weight:400;font-size:30px;line-height:36px">${escape(title)}</h1>
<div style="font-size:16px;line-height:24px;color:#221E1A">${bodyHtml}</div>${button}
<p style="margin:28px 0 0;font-size:12px;line-height:16px;color:#736859">You received this because of an action in your Tuulit account. Tuulit never puts health details in emails.</p>
</td></tr></table></td></tr></table></body></html>`;
}

export const templates = {
  welcome: (name: string) => ({
    subject: "Welcome to Tuulit",
    html: layout(name ? `Welcome, ${name}` : "Welcome to Tuulit",
      "<p>Your account is ready. Your logs stay encrypted on your phone; your account lets you back them up, use Ask and connect with someone you trust.</p><p>You can delete your account and everything in it at any time from You → Account.</p>"),
  }),
  deleted: () => ({
    subject: "Your Tuulit account was deleted",
    html: layout("Your account is deleted", "<p>Your account, cloud backup, voice-note backups and connections have been removed. Thank you for trying Tuulit.</p><p>If you didn't do this, reply to this email.</p>"),
  }),
};

export async function sendEmail(env: Env, to: string, t: { subject: string; html: string }, replyTo?: { email: string; name: string }): Promise<void> {
  const key = requireSecret("BREVO_API_KEY", env.BREVO_API_KEY, "Email isn't set up on our server yet. Please try again later.");
  const res = await fetch("https://api.brevo.com/v3/smtp/email", {
    method: "POST",
    headers: { "content-type": "application/json", accept: "application/json", "api-key": key },
    body: JSON.stringify({ sender: { email: env.BREVO_SENDER_EMAIL, name: env.BREVO_SENDER_NAME }, to: [{ email: to }], subject: t.subject, htmlContent: t.html, tags: ["transactional"], ...(replyTo ? { replyTo } : {}) }),
  });
  if (!res.ok) {
    // Log Brevo's error code and message with any address scrubbed; never the email body.
    let code = "", message = "";
    try { const j = (await res.json()) as { code?: string; message?: string }; code = j.code ?? ""; message = (j.message ?? "").replace(/[^\s@]+@[^\s@]+/g, "<address>").slice(0, 300); } catch { /* not JSON */ }
    console.warn(JSON.stringify({ event: "brevo_error", status: res.status, code, message }));
    throw new HttpError(502, "The email couldn't be sent. Please try again later.");
  }
  console.log(JSON.stringify({ event: "brevo_sent", subject: t.subject }));
}
