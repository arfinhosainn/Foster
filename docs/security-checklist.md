# Foster — Security Runbook

Companion to `README → Local secrets setup`. This repo is **public** — every
checklist item here exists because of that. Working through it top to bottom is
the whole job; items marked **(dashboard)** are Supabase/Google/GitHub console
actions only you can do, everything else is already in the repo.

## 0. Guardrails (done in-repo)

- [x] `.gitignore`: `Secrets.xcconfig`, `*.jks`, `*.keystore`, `*.p12`, `*.mobileprovision`, `.env*`
- [ ] **(GitHub)** Settings → Code security → enable **Secret Scanning** + **Push Protection** (free on public repos)

## 1. Injection scaffolding — proven with the old key (done in-repo)

All client credentials now resolve at build time and bootstrap via
`Secrets.configure(...)` — Android from `BuildConfig` (Gradle: env var →
`local.properties` → hard failure at configuration time), iOS from Info.plist
entries injected by the gitignored `Secrets.xcconfig` (included via
`#include?` from `Config.xcconfig`; URLs escaped as `https:$()//…`).

**Validation you already get for free:** the currently-seeded local values are
the *old, already-public* anon JWT — the plumbing is proven with a key that has
nothing left to lose. If anything leaks through the scaffolding now, it's
costless by construction.

- [ ] Android: `./gradlew :androidApp:assembleDebug` — builds only with secrets present
- [ ] iOS: build in Xcode after creating `Secrets.xcconfig`
- [ ] App boots and signs in (old key) on both platforms

## 2. Key migration — mint, swap, sweep, retire

The legacy anon JWT is burned into public git history forever; retirement is
the only real fix, and pre-launch (zero installed clients) it costs one
local-file line. Prefer the new-style keys — they support overlapping rotation
with no downtime.

1. **(dashboard)** Supabase → Settings → API: create the **`sb_publishable_…`**
   key (client) and **`sb_secret_…`** key (server).
2. **Swap client:** replace `supabase.publishable_key` in `local.properties`
   and `SUPABASE_PUBLISHABLE_KEY` in `Secrets.xcconfig`. Rebuild, sign in, done
   — rotation is now exactly this one-line edit forever.
3. **Swap server:** `supabase secrets set SUPABASE_SERVICE_ROLE_KEY=<sb_secret_…>`
   (or keep the service-role JWT — both work server-side; the point is it was
   never in the repo) and confirm `GEMINI_API_KEY` is set. Redeploy both
   functions: `supabase functions deploy delete-account --no-verify-jwt` and
   `supabase functions deploy brainstorm --no-verify-jwt`.
4. **Consumer sweep BEFORE retiring the legacy JWT** — anything still holding
   it hard-fails at retirement: TestFlight builds, internal-track APKs,
   Postman/Insomnia collections, teammates' checkouts (`local.properties`,
   `Secrets.xcconfig`), CI variables.
5. **(dashboard)** Watch Supabase API logs until old-key traffic reads zero,
   then retire the legacy anon key (Settings → API → legacy keys → retire).
   This rotates the JWT secret and is what kills the public-history copy.
6. **Kill-proof:** with the retired key —
   ```bash
   curl -s -o /dev/null -w "%{http_code}" \
     https://ulrzuzrwilemkcahsvih.supabase.co/auth/v1/settings \
     -H "apikey: <retired-anon-key>"
   ```
   must return **401**. That is the proof the leaked copy is worthless.
7. **(dashboard)** RevenueCat: create the real `goog_`/`appl_` public keys,
   paste into `local.properties` / `Secrets.xcconfig` only.
8. **(Google Cloud)** No rotation needed for the OAuth client IDs (public
   identifiers) — but verify the Android client is restricted to your package
   name + release SHA-1 and iOS to your bundle ID.

## 3. Brainstorm cost-abuse defenses (the real-money risk)

Public publishable key + OAuth open signups + a per-user gate = N accounts →
N× Gemini calls. Layers, in binding order:

| Layer | Value | Where |
|---|---|---|
| Per-user cooldown | 1/contact/day (pre-existing) | edge function |
| **Per-IP** | 30/day (`PER_IP_DAILY_LIMIT`) | edge function + `brainstorm_ip_limits` |
| **Global ceiling** | 1,000/day (`GLOBAL_DAILY_CEILING`) — backstop only | edge function |
| Alerts | edge-triggered POST at 50% / 80% to `ALERT_WEBHOOK_URL` | edge function |
| Hard budget cap | **(Google AI Studio)** set a spend limit on the Gemini key — the actual kill-switch | dashboard |

Deployment steps:

1. Run `onboarding/sql/brainstorm_rate_limits.sql` (creates the deny-all-RLS
   `brainstorm_ip_limits` + `brainstorm_alerts` tables and the pg_cron nightly
   purge; needs the `pg_cron` extension, enabled by the script).
2. **(dashboard)** Optional secrets: `ALERT_WEBHOOK_URL` (Resend / Slack /
   Discord / Zapier→email) so the 50%/80% alerts reach you.
3. **IP-header probe (do this once, empirically):** set
   `supabase secrets set DEBUG_IP_HEADERS=true`, redeploy, generate one
   brainstorm, and read the `[ip-probe]` lines in the function logs. Confirm the
   chosen extraction (`cf-connecting-ip` → right-most `x-forwarded-for`) is the
   real caller — if the right-most entry is an internal proxy address, switch
   `extractClientIp` to second-from-right. Then **remove the probe secret**.
4. **(dashboard)** Auth → Rate Limits: tighten signup/token endpoints. Enable
   bot protection (Turnstile) as defense-in-depth — note the client signs up via
   Google/Apple OAuth only, so the function-level limits above are the levers
   that actually bind today. Email confirmation matters only if email signup
   ships.

## 4. Server posture verification

1. **Avatars bucket:** run `onboarding/sql/avatars_bucket_policies.sql` —
   owner-scoped insert/delete (`storage.foldername(name)[1] = auth.uid()::text`)
   plus bucket-level MIME allowlist + 2 MB size limit. Verify in Dashboard →
   Storage that no other loose policies remain on the bucket.
2. **RLS proof** — with only the publishable key and no session, every public
   table must deny:
   ```bash
   curl -s -o /dev/null -w "%{http_code}\n" \
     "https://ulrzuzrwilemkcahsvih.supabase.co/rest/v1/contacts?select=id" \
     -H "apikey: <publishable-key>" -H "Authorization: Bearer <publishable-key>"
   # expect 401/403 (or an empty RLS-filtered result) — never data
   ```
   Repeat for `check_ins`, `notes`, `brainstorm_sessions`, `subscriptions`.
3. **Secrets confirmation** — `GEMINI_API_KEY` and the service-role /
   `sb_secret_` key exist only via `supabase secrets list` (never in the repo,
   never client-side). `delete-account` is deployed `--no-verify-jwt` but
   verifies the caller's JWT itself — accepted design.
4. **Docs hygiene:** `DB_IMPLEMENTATION_STATUS.md` still names the project URL
   (harmless — it's public in every binary anyway).