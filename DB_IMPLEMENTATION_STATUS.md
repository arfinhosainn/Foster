# Foster — Current Database Implementation (Status)

> Status snapshot of the Supabase database. The migration toward the new ERD
> (`migration_v2_erd.sql`) has been **applied and verified live**.
>
> Reference ERD: `~/Desktop/supabase_schema_erd (1).html`
> Migration: `onboarding/sql/migration_v2_erd.sql` (current SQL — matches what is in Supabase)

## 1. Overview

Foster is a Kotlin Multiplatform (Compose) app with modules: `androidApp`, `shared`, `onboarding`, `home`.
The only backend integration so far is **Supabase** and it is used **only by onboarding**.
The `home` module is UI-only (sample/static data, no DB calls yet).

- Project ref: `https://ulrzuzrwilemkcahsvih.supabase.co`
- Supabase client config: `onboarding/src/commonMain/kotlin/app/usefoster/onboarding/data/supabase/SupabaseConfig.kt`
- Auth: Google OAuth via `supabase-kt` `ComposeAuth.googleNativeLogin(serverClientId = <Google web client id>)`
- Installed plugins on the client: `Auth`, `Postgrest`, `Storage`

### Two-layer story

| Layer | Status |
|---|---|
| Onboarding tables (6) | **Implemented** — created by `onboarding/sql/complete_onboarding.sql`, used by the app |
| New design layer (contacts, check-ins, reminders, brainstorm, badges, subscriptions) + naming standardization | **Applied & verified live** — `onboarding/sql/migration_v2_erd.sql`. Adds new tables, standardizes ownership column to `owner_id`, migrates+drops `user_reminder_preferences`, rewrites the RPC. App-facing contract (RPC payload + profiles reads) unchanged |

---

## 2. Implemented — Onboarding Tables (6)

Schema source: `onboarding/sql/complete_onboarding.sql` (latest) +
migration `onboarding/sql/migration_email.sql` (switched auth from phone → email).

### profiles
One row per authenticated user, keyed on `auth.users.id`.

| column | type | notes |
|---|---|---|
| id | uuid PK | `references auth.users(id) on delete cascade` |
| email | text | added by migration |
| email_verified | boolean not null default false | added by migration |
| display_name | text | from onboarding Name step (maps to ERD `full_name`) |
| contact_name | text | from onboarding Contact step |
| avatar_url | text | avatar image URL |
| selected_avatar_id | text | chosen built-in avatar id |
| onboarding_step | integer not null default 0 | index of `OnboardingStep` enum (9 = complete); check 0–9 |
| onboarding_completed_at | timestamptz | set when onboarding completes |
| full_name | text | ERD-aligned; backfilled from `display_name`; written by RPC (applied) |
| default_frequency | text | ERD-aligned; lowercase enum; written by RPC (applied) |
| default_reminder_time | time | ERD-aligned; written by RPC (applied) |
| subscription_tier | text | ERD-aligned; kept in sync by `sync_subscription_tier` trigger (applied) |
| created_at / updated_at | timestamptz | `updated_at` auto-set by trigger |

> `phone_number` / `phone_verified` were **dropped** in `migration_email.sql`.

### groups
User-created audience groups. (`owner_user_id` was renamed to `owner_id` by the applied migration to standardize naming.)

| column | type | notes |
|---|---|---|
| id | uuid PK | default gen_random_uuid() |
| owner_id | uuid not null | `references auth.users(id) on delete cascade`; indexed |
| name | text not null | |
| color | text | |
| created_at / updated_at | timestamptz | `updated_at` auto-set by trigger |

### user_reminder_preferences
One row per user for the recurring check-in reminder time.

| column | type | notes |
|---|---|---|
| user_id | uuid PK | `references auth.users(id) on delete cascade` |
| reminder_frequency | text | enum name string (e.g. `DAILY`, `WEEKLY`, ...) |
| reminder_hour | integer | |
| reminder_minute | integer | |
| created_at / updated_at | timestamptz | |

> **Replaced by the applied migration**: data was backfilled into `profiles.default_frequency` / `default_reminder_time`, then this table was dropped. Verified: no longer exists in Supabase.

### custom_reminders
User-defined reminders. This is the onboarding-era stand-in for the ERD's `REMINDERS` table.

| column | type | notes |
|---|---|---|
| id | uuid PK | default gen_random_uuid() |
| owner_id | uuid not null | `references auth.users(id) on delete cascade`; indexed |
| title | text not null | |
| description | text not null default '' | |
| recurrence | text not null default 'none' | check constraint: none/daily/weekly/biweekly/monthly/semiannually/annually |
| date_epoch_millis | bigint | nullable — ERD models this as `event_date date` (open decision) |
| contact_id | uuid null | added by applied migration (ERD: contact-level) |
| created_at / updated_at | timestamptz | `updated_at` auto-set by trigger |

### notes
Notes added during the Add Note onboarding step.

| column | type | notes |
|---|---|---|
| id | uuid PK | default gen_random_uuid() |
| owner_id | uuid not null | `references auth.users(id) on delete cascade`; indexed |
| title | text not null | |
| body | text not null default '' | ERD calls this `description` (open decision) |
| contact_id | uuid null | added by applied migration (ERD: contact-level) |
| created_at / updated_at | timestamptz | `updated_at` auto-set by trigger |

### notification_settings
One row per user. Not present in the new ERD — kept for now (open decision).

| column | type | notes |
|---|---|---|
| user_id | uuid PK | `references auth.users(id) on delete cascade` |
| permission_asked | boolean not null default false | |
| permission_granted | boolean not null default false | |
| created_at / updated_at | timestamptz | `updated_at` auto-set by trigger |

---

## 3. Migration — New ERD Layer (`migration_v2_erd.sql`)
Applied and verified live (2026-08-01): the schema in Supabase matches this file. Standardizes the ownership column name to `owner_id` across the whole schema (groups/notes/custom_reminders get renamed), migrates + drops `user_reminder_preferences`, and rewrites the `complete_onboarding` RPC. The app-facing contract is preserved: the RPC payload shape and the `profiles` reads are unchanged.
Adds the ERD's relationship-tracker core plus cross-cutting fixes.

### Cross-cutting fixes (in the applied migration)
1. **Ownership naming standardized to `owner_id`** — `groups`, `notes`, `custom_reminders` renamed from `owner_user_id`; their RLS policies recreated; `complete_onboarding` updated.
2. **FK-column indexes** on `groups(owner_id)`, `notes(owner_id)`, `custom_reminders(owner_id)`, plus new-table FKs.
3. **`set_updated_at()` trigger function** + `before update` triggers on every table with `updated_at`.
4. **Check constraints** on enum-shaped text: `custom_reminders.recurrence` (lowercase, matches the Kotlin `ReminderFrequency` enum), `profiles.onboarding_step` (0–9), `profiles.default_frequency` (lowercase).
5. **ERD-aligned `profiles` columns**: `full_name` (backfilled), `default_frequency`, `default_reminder_time`, `subscription_tier`. Written by the updated RPC / subscription trigger (no longer frozen).
6. **`user_reminder_preferences` migrated into profiles then dropped** — one source of truth for reminder cadence.

### New tables

**contacts** — the hub entity.
| column | type |
|---|---|
| id | uuid PK |
| owner_id | uuid not null FK → auth.users |
| name | text not null |
| avatar_color | text |
| check_in_frequency | text default 'none' (check: none/daily/weekly/biweekly/monthly) |
| reminder_time | time |
| next_check_in_date | date |
| last_check_in_date | date |
| streak_count | int default 0 |
| created_at / updated_at | timestamptz |

Indexes: `(owner_id)`, `(next_check_in_date)`. RLS: owner-based (select/insert/update/delete). Trigger: updated_at.

**contact_groups** — M:N `contacts` ↔ `groups`.
- PK `(contact_id, group_id)`, FKs cascade. Index on `group_id`.
- RLS insert checks **both** the contact and the group belong to the caller (can't link across owners).

**check_ins** — the event log.
- `id PK`, `contact_id` FK, `checked_in_at timestamptz`, `note text`.
- Index `(contact_id, checked_in_at desc)`.
- Matches the ERD exactly (no denormalized owner column); RLS resolves ownership through `contacts`.

**brainstorm_sessions** — `id PK`, `contact_id` FK, `created_at`.

**brainstorm_topics** — `id PK`, `session_id` FK, `icon`, `title`, `description`.

**badges** — public catalog: `id PK`, `name`, `threshold`. RLS: read-only for authenticated users.

**user_badges** — unlocks: PK `(owner_id, badge_id)`, `unlocked_at`. RLS owner-based.

**subscriptions** — `id PK`, `owner_id` FK, `plan`, `status`, `trial_ends_at`, timestamps. RLS owner-based.

### contact_id wiring (applied)
- `notes.contact_id` (nullable FK → contacts, indexed).
- `custom_reminders.contact_id` (nullable FK → contacts, indexed).

> **Verified live:** `user_reminder_preferences` no longer exists; all 8 new
> tables exist; `profiles` carries `full_name`/`default_frequency`/
> `default_reminder_time`/`subscription_tier` with migrated data; notes/reminders
> expose `owner_id` and `contact_id`.

---

## 4. Row Level Security

RLS is enabled on **all** implemented tables (6 onboarding) and all new tables in the applied migration.

- `profiles` → keyed on `id`.
- `groups` / `custom_reminders` / `notes` → keyed on `owner_id` (renamed from `owner_user_id` by the applied migration).
- `user_reminder_preferences` / `notification_settings` → keyed on `user_id` (`user_reminder_preferences` was dropped by the applied migration).
- `contacts` / `subscriptions` / `user_badges` → keyed on `owner_id`.
- `contact_groups`, `check_ins`, `brainstorm_sessions`, `brainstorm_topics` → ownership resolved via `contacts` (subquery/join).
- `badges` → public read for authenticated users only.

Update policies use both `using` **and** `with check` so a user can't reassign ownership to someone else.

---

## 5. RPC Function: `complete_onboarding(payload jsonb)`

Single database transaction (`security definer`), called once when onboarding finishes.
Defined in `onboarding/sql/complete_onboarding.sql`. Behavior:

1. `v_user_id := auth.uid()`; raises `'Not authenticated'` if null.
2. **Upsert `profiles`** (`onboarding_step = 9`, `onboarding_completed_at = now()`, and — since the applied migration — also `full_name`, `default_frequency` (lowercased), `default_reminder_time`).
3. **Replace `groups`**.
4. **Replace `custom_reminders`** (writes `contact_id` if the payload provides one).
5. **Replace `notes`** (writes `contact_id` if provided).
6. **Upsert `notification_settings`**.

The applied migration rewrote this RPC to (a) use `owner_id`, (b) write the new `profiles` columns so they don't go stale, (c) normalize `reminderFrequency` to lowercase, (d) drop the `user_reminder_preferences` write, and (e) **seed non-destructively** — it is a no-op for accounts that already completed onboarding, and never blanket-deletes existing groups/notes/reminders. It does **not** yet write the new ERD tables (`contacts`, `check_ins`, etc.).

Payload-key notes (verified against `OnboardingDtos.kt`, not guessed):
- `reminderTime` is **flat** — the domain `ReminderTimeDraft{hour, minute}` is flattened to top-level `reminderHour`/`reminderMinute` in `CompleteOnboardingPayload`, so the RPC reads `payload->>'reminderHour'` / `payload->>'reminderMinute'`.
- `reminderFrequency` arrives UPPERCASE (enum `.name`) and is `lower()`-ed in the RPC; `customReminders[].recurrence` already arrives lowercase.
- `customReminders[]` / `notes[]` send **no** `contactId` today.
- **Security**: because the function is `security definer` (RLS bypassed), any `contactId` in the payload is validated against the caller's own contacts via a `left join ... and c.owner_id = v_user_id` — a foreign contact id silently becomes NULL.

---

## 6. Supabase Storage

- Bucket: `avatars`
- Upload path: `avatars/{user_id}/profile.{ext}`
- `SupabaseOnboardingProfileDataSource.uploadAvatar(bytes, extension)` exists and returns a public URL.
- **Not wired to any UI yet** — the Contact step stores the built-in avatar id and sets `profilePhotoUri = null`.

---

## 7. Kotlin Data Layer

### Domain (`onboarding/src/commonMain/kotlin/app/usefoster/onboarding/domain/`)
- `OnboardingProfileDataSource` interface:
  - `submitOnboarding(draft: OnboardingDraft): EmptyResult<OnboardingProfileError>`
  - `getOnboardingStep(): Result<OnboardingStep?, OnboardingProfileError>`
  - `ensureProfileExists(): EmptyResult<OnboardingProfileError>`
- `OnboardingProfileError` + `toUserMessage()` mapping.
- `OnboardingStep(index)` enum:
  `Welcome(0) Name(1) Contact(2) Group(3) DayReminder(4) TimeReminder(5) CustomReminder(6) AddNote(7) Notification(8) Complete(9)`
- `OnboardingDraft` / `GroupDraft` / `CustomReminderDraft` / `ReminderTimeDraft` / `NoteDraft` / `ReminderFrequency` enums.

### Supabase implementation (`data/supabase/`)
- `SupabaseOnboardingProfileDataSource`:
  - `submitOnboarding` → encodes `CompleteOnboardingPayload` JSON → `postgrest.rpc("complete_onboarding", {payload})`.
  - `getOnboardingStep` → `select single` from `profiles` → if `onboarding_completed_at != null` → `Complete`, else map `onboarding_step` index to enum.
  - `ensureProfileExists` → `upsert` `{id: userId, onboarding_step: 1}` with `onConflict("id")`.
  - `uploadAvatar` (unused) → storage upload + `publicUrl`.
- DTOs (`dto/OnboardingDtos.kt`): `CompleteOnboardingPayload`, `GroupDto`, `CustomReminderDto`, `NoteDto`, `OnboardingStepResponse`.
- Client: `SupabaseConfig` + `createAppSupabaseClient()`.

### Local persistence (device-only, not Supabase)
- `OnboardingDraftStore` (`presentation/OnboardingDraftStore.kt`) — in-memory `StateFlow` backed by a local data source.
- `OnboardingDraftLocalDataSource` (`domain/`):
  - Android: **DataStore Preferences**, key `onboarding_draft_json`.
  - iOS: **NSUserDefaults**, key `onboarding_draft_json`.
- Draft stored as JSON of `OnboardingDraft` (`data/OnboardingDraftJson.kt`).

---

## 8. Write Flow (how onboarding reaches the DB)

1. Each screen ViewModel writes to the local `OnboardingDraftStore` and advances `currentStep`.
2. Final submit from **`AddNoteViewModel`** (Next/Skip) or **`NotificationViewModel`** (Turn on / Skip): sets `currentStep = Complete`, calls `submitOnboarding(draft)`.
3. `SupabaseOnboardingProfileDataSource.submitOnboarding` calls the `complete_onboarding` RPC.
4. On success: `draftStore.clear()` + navigate to Home.
5. On error: keep the local draft, surface a retryable error.

## 9. Read / Resume / Re-login Flow (app startup)

`OnboardingApp.kt`:
- **Both** app-launch routing and post-Google-sign-in routing call the shared `routeAfterAuth()` (`OnboardingApp.kt`). It calls `getOnboardingStep()` and routes via `OnboardingStep.toScreen()`:
  - `onboarding_completed_at` set → `Screen.Home` (data intact, no re-onboarding).
  - saved step index → resumes onboarding at that step.
  - read error → `ensureProfileExists()` then `Screen.Name`.
- This fixes the reinstall case: a returning user signs in with Google, resolves to the same `auth.uid()`, and is routed straight to Home with all their data — never re-runs onboarding.
- App launch path: if `currentSessionOrNull() != null` → `routeAfterAuth()`; else Welcome → Google sign-in → `onGoogleSignInSuccess` → `routeAfterAuth()` (same function, can't drift).

---

## 10. Reconciliation Map (current → new ERD)

| Current | New design |
|---|---|
| profiles.display_name | profiles.full_name (written in sync by RPC) |
| user_reminder_preferences | profiles.default_frequency / default_reminder_time (migrated + dropped) |
| custom_reminders | REMINDERS (contact-level; `event_date date`) |
| notes (owner-level, `body`) | NOTES (contact-level, `description`) |
| groups.owner_user_id | groups.owner_id (renamed) |
| profiles.contact_name | ??? — not in ERD; open decision (see below) |
| notification_settings | not in ERD — keep / fold into profiles (TBD) |
| (new) | contacts, contact_groups, check_ins, brainstorm_sessions, brainstorm_topics, badges, user_badges, subscriptions |

---

## 11. Open Decisions / TODOs

1. **Global vs contact-only notes/reminders** — `contact_id` is nullable in the migration; decide if no-contact items stay a supported concept, then backfill + make `NOT NULL`.
2. **`custom_reminders` → `reminders` rename** with `date_epoch_millis bigint` → `event_date date` (breaking: touches Kotlin DTOs).
3. **`profiles.contact_name`** — was the onboarding "emergency contact" field; not in the ERD and now that a real `contacts` table exists, decide whether it maps to a contact, is retired, or stays.
4. **`notification_settings`** — not in the ERD; keep it, or fold permission flags into `profiles`.
5. **Denormalized contact caches** — `contacts.streak_count` / `last_check_in_date` / `next_check_in_date` must be updated on every `check_ins` insert (app code or trigger), or they drift.
6. **`subscription_tier` cache** — handled by the `sync_subscription_tier` trigger, which fires on `insert or update of plan, status` and folds status in (`active`/`trialing` → plan, anything else → `'free'`); verify once subscriptions go live.
7. **`brainstorm_topics` two-hop RLS** — test the topic→session→contact→owner policy once live; consider denormalizing `owner_id` if AI-generated topics are high-volume.
8. **Casing discipline** — `default_frequency`/`recurrence` are stored lowercase (normalized by the RPC); keep every new writer consistent.
9. **Home screen still on sample data** — no reads against `contacts`/`check_ins` yet.
10. **Avatar upload** — `uploadAvatar` implemented but not called from the UI.
11. **`complete_onboarding` RPC** — extended for profiles columns + contact-ownership validation + non-destructive seeding; still needs `contacts`/`check_ins` writes once the app creates them.

## 12. Key Files

| File | Purpose |
|---|---|
| `onboarding/sql/complete_onboarding.sql` | Implemented: tables + RLS + RPC |
| `onboarding/sql/migration_email.sql` | phone → email migration (applied) |
| `onboarding/sql/migration_v2_erd.sql` | **Applied & verified live**: fixes + new ERD layer |
| `onboarding/src/commonMain/kotlin/app/usefoster/onboarding/data/supabase/SupabaseConfig.kt` | Supabase URL/keys, client factory |
| `onboarding/src/commonMain/kotlin/app/usefoster/onboarding/data/supabase/SupabaseOnboardingProfileDataSource.kt` | All Supabase reads/writes |
| `onboarding/src/commonMain/kotlin/app/usefoster/onboarding/data/supabase/dto/OnboardingDtos.kt` | Request/response DTOs |
| `onboarding/src/commonMain/kotlin/app/usefoster/onboarding/domain/OnboardingProfileDataSource.kt` | Domain interface |
| `onboarding/src/commonMain/kotlin/app/usefoster/onboarding/domain/OnboardingDraft.kt` | Local draft model |
| `onboarding/src/commonMain/kotlin/app/usefoster/onboarding/presentation/OnboardingDraftStore.kt` | Local draft store |
| `onboarding/src/androidMain/.../DataStoreOnboardingDraftDataSource.kt` | Android local persistence |
| `onboarding/src/iosMain/.../NSUserDefaultsOnboardingDraftDataSource.kt` | iOS local persistence |
