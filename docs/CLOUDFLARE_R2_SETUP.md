# Cloudflare Worker and R2 setup

The Worker (`backend/worker`) is the only server. R2 stores voice-journal recordings that users choose to back up. Structured data stays in Firestore. R2 is used for media only.

## 1. Account and tools

1. Create a Cloudflare account. The Workers free plan and R2's free tier are enough to start: 10 GB stored, 1M writes and 10M reads a month.
2. Run:
   ```
   cd backend/worker
   npm install
   npx wrangler login
   ```

## 2. R2 bucket (private)

```
npx wrangler r2 bucket create tuulit-media
```

- Leave **public access off**. Don't attach a custom domain or `r2.dev` URL. Objects are only reachable through the Worker's signed URLs.
- No R2 API token is needed. The Worker uses the `MEDIA` binding in `wrangler.toml`.

## 3. KV namespace for rate limits

```
npx wrangler kv namespace create RATE
```

Copy the printed `id` into `wrangler.toml` under `[[kv_namespaces]]`.

## 4. Secrets

```
npx wrangler secret put AI_API_KEY                 # AI provider key (Anthropic by default)
npx wrangler secret put BREVO_API_KEY              # see BREVO_SETUP.md
npx wrangler secret put FIREBASE_SERVICE_ACCOUNT   # see FIREBASE_SETUP.md, step 7
npx wrangler secret put MEDIA_SIGNING_KEY          # 32+ random characters, e.g. openssl rand -base64 48
```

## 5. Variables (`wrangler.toml` → `[vars]`)

| Var | Meaning |
|---|---|
| `FIREBASE_PROJECT_ID`, `FIREBASE_PROJECT_NUMBER` | From Firebase project settings |
| `APP_CHECK_MODE` | `monitor` to start, `enforce` once Play Integrity works |
| `AI_PROVIDER` | `anthropic`, or `openai_compatible` (also set `AI_BASE_URL`) |
| `AI_MODEL` | Model name for that provider (default `claude-haiku-5-5`) |
| `AI_DAILY_LIMIT` | Questions per user per day (default 60) |
| `PUBLIC_BASE_URL` | This Worker's URL, used to build signed media links |
| `BREVO_SENDER_EMAIL`, `BREVO_SENDER_NAME` | Verified Brevo sender |

## 6. Deploy and connect the app

```
npm test          # unit tests
npm run typecheck
npm run deploy
curl https://tuulit-api.<subdomain>.workers.dev/health   # {"ok":true}
```

Then tell the app where the Worker is. Add this to `local.properties` (git-ignored) or the CI environment:

```
TUULIT_API_BASE_URL=https://tuulit-api.<subdomain>.workers.dev
```

Rebuild the app. Ask, voice backup, Connect pushes and branded emails are now live.

## How uploads stay private

1. The app asks `POST /v1/media/upload-url` with its Firebase ID token.
2. The Worker checks the type (`audio/mp4`) and size (≤10 MB), creates the key `users/{uid}/voice/{uuid}.m4a`, and returns `…/v1/media/blob?key=…&exp=…&sig=…`. That link expires in 5 minutes and is signed with HMAC-SHA256 over method, key and expiry.
3. The app PUTs the file to that link. The Worker re-checks the signature and size, then writes to R2 through the binding.
4. Downloads work the same way with a GET signature. A PUT link can't be used to read.
5. Users can only ask for links to keys under their own `users/{uid}/` prefix. Account deletion removes the whole prefix.
