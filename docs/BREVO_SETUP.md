# Brevo setup (transactional email)

Brevo sends the Worker's own emails. **The API key never reaches the app.**

| Email | Trigger | Worker route |
|---|---|---|
| Welcome | Account created | `POST /v1/email/welcome` (once per account) |
| Account deleted | Account deletion | Sent by `POST /v1/account/delete` |
| Website contact form | tuulit.app/contact | `POST /v1/contact` (to `CONTACT_TO`) |

**Email verification and password reset do not use Brevo.** Both use Firebase Authentication's built-in emails (see `docs/AUTH_AND_SESSION.md`). There is exactly one verification path, so an email problem in Brevo can never block sign-up or confirmation.

Templates live in `backend/worker/src/email.ts`. They use the Tuulit palette, escape all user input, and **never contain cycle or health details**. A test enforces this.

## Steps

1. Create a Brevo account at https://www.brevo.com. The free plan allows 300 emails a day.
2. Go to **Senders, domains & dedicated IPs** → Domains → add your domain and add the DKIM and DMARC DNS records Brevo shows. Wait until the domain shows as authenticated. Without this, mail from `hello@tuulit.app` is likely to land in spam or be rejected.
3. Add a sender address on that domain (for example `hello@yourdomain`).
4. Go to **SMTP & API** → API keys → Generate a new API key named `tuulit-worker`.
5. Store it as a Worker secret. **Paste the key as text.** On Windows, a Ctrl+V that the terminal doesn't support is stored as a one-character control code; that is what broke every Brevo email before 2.3.0. Piping avoids it:
   ```
   cd backend/worker
   printf '%s' 'xkeysib-…' | npx wrangler secret put BREVO_API_KEY
   ```
6. In `wrangler.toml` set `BREVO_SENDER_EMAIL` to the verified sender and keep `BREVO_SENDER_NAME = "Tuulit"`.
7. Deploy (`npm run deploy`), then create a test account in the app and check the welcome email arrives.

## Logging and errors

- A malformed key (shorter than 20 characters, or with spaces or control characters) is refused before any request with `{event: "secret_invalid", name: "BREVO_API_KEY", length}`. The value itself is never logged.
- When Brevo refuses an email, the Worker logs `{event: "brevo_error", status, code, message}`. Addresses in Brevo's message are replaced with `<address>`; bodies are never logged. A successful send logs `brevo_sent` with the subject only. View the logs with `npx wrangler tail`.
- A failed email returns 502 with a friendly message. The app treats the welcome email as best-effort.
