# Nekko — Missing Kotlin Table Models

> Context for future sessions: the ERD migration (`onboarding/sql/migration_v2_erd.sql`)
> has been **applied and verified live** in Supabase. The 8 new tables have **no
> Kotlin models yet** — the app cannot read or write them. Models to be created
> later.

## DB state (verified live 2026-08-01)

All migration changes are live:
- `user_reminder_preferences` dropped
- `groups` / `notes` / `custom_reminders` renamed `owner_user_id` → `owner_id`
- `profiles` gained `full_name`, `default_frequency`, `default_reminder_time`, `subscription_tier`
- `notes` / `custom_reminders` gained nullable `contact_id` (FK → contacts)
- New tables: `contacts`, `contact_groups`, `check_ins`, `brainstorm_sessions`, `brainstorm_topics`, `badges`, `user_badges`, `subscriptions`

## Missing Kotlin table models (create later)

| DB table | Kotlin model | Notes |
|---|---|---|
| `contacts` | none | hub entity: id, owner_id, name, avatar_color, check_in_frequency, reminder_time, next_check_in_date, last_check_in_date, streak_count |
| `contact_groups` | none | M:N contacts↔groups, PK (contact_id, group_id) |
| `check_ins` | none | id, contact_id, checked_in_at, note |
| `brainstorm_sessions` | none | id, contact_id, created_at |
| `brainstorm_topics` | none | id, session_id, icon, title, description |
| `badges` | none | id, name, threshold (public catalog) |
| `user_badges` | none | PK (owner_id, badge_id), unlocked_at |
| `subscriptions` | none | id, owner_id, plan, status, trial_ends_at |

Also missing for modified tables:
- **`profiles`** — only `OnboardingStepResponse` (2 columns) exists; no full `Profile` model for `full_name`, `default_frequency`, `default_reminder_time`, `subscription_tier`.
- **`notes` / `custom_reminders`** — `NoteDto` / `CustomReminderDto` are write-only; no `contactId`, no response models.

## What exists today (onboarding-only)

- `onboarding/src/commonMain/kotlin/app/usenekko/onboarding/data/supabase/dto/OnboardingDtos.kt` — `CompleteOnboardingPayload`, `GroupDto`, `CustomReminderDto`, `NoteDto`, `OnboardingStepResponse`
- Domain drafts: `GroupDraft`, `CustomReminderDraft`, `NoteDraft`, `ReminderTimeDraft`, `ReminderFrequency`, `OnboardingDraft`
- `home` is UI-only, still on sample data (`CheckinGridSample.kt`) — no DB reads

## Key files

- Migration (applied): `onboarding/sql/migration_v2_erd.sql`
- Status doc: `DB_IMPLEMENTATION_STATUS.md`
- Supabase client: `onboarding/src/commonMain/kotlin/app/usenekko/onboarding/data/supabase/SupabaseConfig.kt`
