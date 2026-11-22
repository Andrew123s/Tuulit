# tuulit-api (Cloudflare Worker)

The only server for Tuulit. It holds every secret and does the work Firebase Spark can't (no Cloud Functions).

| Route | Auth | Purpose |
|---|---|---|
| `GET /health` | none | Liveness |
| `POST /v1/ai` | ID token | Ask: `{tool: chat\|explain\|myth\|appointment, messages, context?, locale?}` returns `{text, urgency, verdict?}` |
| `POST /v1/email/welcome` | ID token | Welcome email (once) |
| `POST /v1/email/verification` | ID token | Branded verification email (5 per day) |
| `POST /v1/media/upload-url` | ID token | Signed PUT URL for a voice note |
| `POST /v1/media/download-url` | ID token | Signed GET URL for the user's own key |
| `POST /v1/media/delete` | ID token | Delete one of the user's objects |
| `PUT/GET /v1/media/blob` | HMAC signature | The signed URL itself |
| `POST /v1/connect/notify` | ID token | FCM push to a connected person (`sos`, `reply`, `accepted`) |
| `POST /v1/account/delete` | ID token, fresh sign-in | Delete all of the user's cloud data and the auth user |

## Develop

```
npm install
npm test            # vitest
npm run typecheck
npx wrangler dev    # needs a .dev.vars file with the secrets for local runs (git-ignored)
```

## Configure and deploy

See `docs/CLOUDFLARE_R2_SETUP.md` (bucket, KV, secrets, vars), `docs/FIREBASE_SETUP.md` (service account) and `docs/BREVO_SETUP.md`.

## Switching AI provider

Set `AI_PROVIDER` to `anthropic` (default) or `openai_compatible` (also set `AI_BASE_URL`), set `AI_MODEL`, and put that provider's key in the `AI_API_KEY` secret. No app release is needed.
