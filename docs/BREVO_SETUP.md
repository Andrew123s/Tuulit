# Brevo setup (transactional email)

Brevo sends three emails, all through the Worker. **The API key never reaches the app.**

| Email | Trigger | Worker route |
|---|---|---|
| Welcome | Account created | `POST /v1/email/welcome` (once per account) |
| Confirm your email | Account created, and "Send again" in You → Account | `POST /v1/email/verification` (5 per day). The Worker generates the Firebase verification link with the service account. |
| Account deleted | Account deletion | Sent by `POST /v1/account/delete` |

Password reset uses Firebase's built-in email, which is free and rate-limited by Google. If the Worker or Brevo is unavailable, verification falls back to Firebase's built-in email, so sign-up never fails because of email.

Templates live in `backend/worker/src/email.ts`. They use the Tuulit palette, escape all user input, and **never contain cycle or health details**. A test enforces this.

## Steps

1. Create a Brevo account at https://www.brevo.com. The free plan allows 300 emails a day.
2. Go to **Senders, domains & dedicated IPs** → Domains → add your domain and add the DKIM and DMARC DNS records Brevo shows. Wait until the domain shows as authenticated.
3. Add a sender address on that domain (for example `hello@yourdomain`).
4. Go to **SMTP & API** → API keys → Generate a new API key named `tuulit-worker`.
5. Store it as a Worker secret:
   ```
   cd backend/worker
   npx wrangler secret put BREVO_API_KEY
   ```
6. In `wrangler.toml` set `BREVO_SENDER_EMAIL` to the verified sender and keep `BREVO_SENDER_NAME = "Tuulit"`.
7. Deploy (`npm run deploy`), then create a test account in the app and check the welcome and confirmation emails arrive.

## Logging and errors

- On failure, the Worker logs only `{event: "brevo_error", status}`. It never logs addresses or bodies. View the logs with `npx wrangler tail`.
- When an email fails, the request returns 502 with a friendly message. The app treats welcome email as best-effort and falls back to Firebase for verification.
