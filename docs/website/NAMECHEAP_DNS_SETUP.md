# Connecting tuulit.app (Namecheap) to Cloudflare Pages

## Current DNS (read on 2026-11-24, public lookup; nothing was changed)

| Type | Name | Value | Meaning |
|---|---|---|---|
| NS | tuulit.app | `dns1/dns2.registrar-servers.com` | Namecheap BasicDNS is in charge |
| A | tuulit.app | `162.255.119.179` | Namecheap parking page |
| CNAME | www | `parkingpage.namecheap.com` | Parking page |
| MX | tuulit.app | `10 mx1.privateemail.com`, `10 mx2.privateemail.com` | **Namecheap Private Email is active. Keep these records.** |
| TXT | tuulit.app | `v=spf1 include:spf.privateemail.com ~all` | SPF for Private Email |
| TXT | tuulit.app | `brevo-code:62ca58…` | Brevo domain verification (started) |

## Why the nameservers move to Cloudflare

Cloudflare Pages can serve the bare domain `tuulit.app` only when the domain's DNS zone is on Cloudflare. A subdomain alone could be pointed at Pages from Namecheap, but the site must live at `https://tuulit.app`. Cloudflare DNS is free. The domain **stays registered at Namecheap**; only the DNS moves.

## Steps

### 1. Add the site to Cloudflare

1. In the Cloudflare dashboard (the same account as the Worker), go to **Add a domain** → `tuulit.app` → **Free** plan.
2. Cloudflare scans the existing records. Check that the list contains **every** record from the table above:
   - both MX records;
   - the SPF TXT record;
   - the `brevo-code` TXT record.

   Add any that are missing by hand.
3. Delete the scanned **parking** records: the A record `162.255.119.179` and the `www` CNAME to `parkingpage.namecheap.com`.
4. Note the two nameservers Cloudflare gives you, for example `xxx.ns.cloudflare.com`.

### 2. Change the nameservers at Namecheap

1. In Namecheap, go to **Domain List** → **tuulit.app** → **Manage** → **Nameservers**.
2. Choose **Custom DNS**, enter the two Cloudflare nameservers, then save (✓).
3. Propagation usually takes minutes, and can take up to 24 hours. Cloudflare emails you when the zone is active.
4. Email keeps working throughout, because the MX and SPF records were copied in step 1.

### 3. Attach the domain to the Pages project

1. Go to **Workers & Pages** → **tuulit-website** → **Custom domains** → **Set up a custom domain**.
2. Add `tuulit.app`. Cloudflare creates the DNS record and the HTTPS certificate automatically.
3. Add `www.tuulit.app` too.
4. Redirect www to the apex: **Rules** → **Redirect Rules** → **Create rule**:
   - **Matches:** hostname equals `www.tuulit.app`
   - **Action:** dynamic redirect to `concat("https://tuulit.app", http.request.uri.path)`, status 301, preserve the query string.
5. Under **SSL/TLS**, set the encryption mode to **Full (strict)** and turn on **Always Use HTTPS**.

`.app` domains are on the browser HSTS preload list, so the site **only** loads over HTTPS. Cloudflare's automatic certificate covers this.

### 4. Finish Brevo domain authentication (so emails from hello@tuulit.app are delivered)

1. In Brevo, go to **Senders, Domains & Dedicated IPs** → **Domains** → **tuulit.app** → **Authenticate**.
2. Brevo shows the remaining records, usually a DKIM record (`brevo1._domainkey` / `brevo2._domainkey` CNAMEs, or a `mail._domainkey` TXT) and a DMARC record.
3. Add them in **Cloudflare DNS**, with the proxy status set to **DNS only** (grey cloud).
4. Change SPF so both senders are allowed. Edit the existing TXT to:
   `v=spf1 include:spf.privateemail.com include:spf.brevo.com ~all`
   A domain must have **only one** SPF record.
5. Add DMARC if Brevo doesn't provide one:
   `_dmarc` TXT `v=DMARC1; p=none; rua=mailto:ycomspacegh@gmail.com`
6. Click **Verify** in Brevo.

### 5. Check

- `https://tuulit.app` and `https://tuulit.app/privacy` load with a valid certificate.
- `https://www.tuulit.app/privacy` redirects to `https://tuulit.app/privacy`.
- Sending and receiving at your Private Email address still works.
- The contact form on `/contact` delivers to `ycomspacegh@gmail.com`.

## Don't

- Don't delete the MX, SPF or `brevo-code` records.
- Don't proxy (orange-cloud) MX-related or DKIM records.
- Don't add a second SPF record.
