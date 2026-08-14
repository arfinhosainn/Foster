-- =============================================================================
-- Nekko Schema v2 — migration toward the new ERD design
-- Reference ERD: ~/Desktop/supabase_schema_erd (1).html
--
-- Run top to bottom in the Supabase SQL editor.
--
-- SCOPE: This is no longer purely additive. It standardizes the ownership
-- column name across the schema (owner_user_id -> owner_id) so every table and
-- RLS policy uses ONE convention, migrates user_reminder_preferences into
-- profiles, and rewrites the complete_onboarding RPC. The app-facing contract
-- is preserved: the RPC payload shape and the profiles reads the app makes
-- are unchanged.
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Reconciliation map (current -> new ERD design)
--   groups.owner_user_id      -> owner_id
--   notes.owner_user_id       -> owner_id
--   custom_reminders          -> REMINDERS (contact-level, event_date date) —
--                                TODO breaking migration: rename table to `reminders`,
--                                make contact_id NOT NULL, date_epoch_millis -> event_date
--   user_reminder_preferences -> profiles.default_frequency / default_reminder_time
--                                (migrated then dropped here)
--   notification_settings     -> not in ERD; kept until product decides
--   notes.body                -> NOTES.description (later rename)
--   profiles.display_name     -> profiles.full_name (written in sync by RPC)
--   NEW: contacts, contact_groups, check_ins, brainstorm_sessions,
--        brainstorm_topics, badges, user_badges, subscriptions
-- ---------------------------------------------------------------------------

-- =============================================================================
-- 1. Standardize ownership column: owner_user_id -> owner_id
--    One convention across the whole schema (matches the ERD). Renames
--    propagate through FK constraints and indexes; RLS policy expressions are
--    separate objects and must be dropped/recreated below.
--
--    This block is intentionally re-run safe. The migration may already have
--    been applied, in which case owner_user_id no longer exists.
-- =============================================================================

do $$
declare
  v_table_name text;
begin
  foreach v_table_name in array array['groups', 'notes', 'custom_reminders'] loop
    if exists (
      select 1
        from information_schema.columns
       where table_schema = 'public'
         and table_name = v_table_name
         and column_name = 'owner_user_id'
    ) then
      if exists (
        select 1
          from information_schema.columns
         where table_schema = 'public'
           and table_name = v_table_name
           and column_name = 'owner_id'
      ) then
        raise exception 'Table public.% has both owner_user_id and owner_id', v_table_name;
      end if;

      execute format(
        'alter table public.%I rename column owner_user_id to owner_id',
        v_table_name
      );
    end if;
  end loop;
end $$;

-- Recreate the RLS policies that referenced the old column name. Instead of
-- guessing policy names, drop ALL policies on these tables via pg_policies,
-- then recreate them explicitly below. This is re-run safe and immune to
-- policy-name drift.
do $$
declare
  pol record;
  v_table_name text;
begin
  foreach v_table_name in array array[
    'groups',
    'custom_reminders',
    'notes',
    'contacts',
    'contact_groups',
    'check_ins',
    'brainstorm_sessions',
    'brainstorm_topics',
    'badges',
    'user_badges',
    'subscriptions'
  ] loop
    for pol in
      select policyname
        from pg_policies
       where schemaname = 'public'
         and tablename = v_table_name
    loop
      execute format('drop policy %I on public.%I', pol.policyname, v_table_name);
    end loop;
  end loop;
end $$;

create policy "Users can read own groups" on public.groups for select using (auth.uid() = owner_id);
create policy "Users can insert own groups" on public.groups for insert with check (auth.uid() = owner_id);
create policy "Users can update own groups" on public.groups for update
  using (auth.uid() = owner_id) with check (auth.uid() = owner_id);
create policy "Users can delete own groups" on public.groups for delete using (auth.uid() = owner_id);

create policy "Users can read own custom reminders" on public.custom_reminders for select using (auth.uid() = owner_id);
create policy "Users can insert own custom reminders" on public.custom_reminders for insert with check (auth.uid() = owner_id);
create policy "Users can update own custom reminders" on public.custom_reminders for update
  using (auth.uid() = owner_id) with check (auth.uid() = owner_id);
create policy "Users can delete own custom reminders" on public.custom_reminders for delete using (auth.uid() = owner_id);

create policy "Users can read own notes" on public.notes for select using (auth.uid() = owner_id);
create policy "Users can insert own notes" on public.notes for insert with check (auth.uid() = owner_id);
create policy "Users can update own notes" on public.notes for update
  using (auth.uid() = owner_id) with check (auth.uid() = owner_id);
create policy "Users can delete own notes" on public.notes for delete using (auth.uid() = owner_id);

-- =============================================================================
-- 2. FK-column indexes
--    Postgres does NOT auto-index FK columns (only the referenced PK side).
--    Every RLS query filters on these.
-- =============================================================================

drop index if exists idx_groups_owner;
drop index if exists idx_notes_owner;
drop index if exists idx_custom_reminders_owner;
create index if not exists idx_groups_owner           on public.groups(owner_id);
create index if not exists idx_notes_owner            on public.notes(owner_id);
create index if not exists idx_custom_reminders_owner on public.custom_reminders(owner_id);
-- user_reminder_preferences / notification_settings: user_id is their PK, already indexed.

-- =============================================================================
-- 3. updated_at automation — stop relying on the app to set this
-- =============================================================================

create or replace function public.set_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_set_updated_at on public.profiles;
create trigger trg_set_updated_at before update on public.profiles
  for each row execute function public.set_updated_at();

drop trigger if exists trg_set_updated_at on public.groups;
create trigger trg_set_updated_at before update on public.groups
  for each row execute function public.set_updated_at();

drop trigger if exists trg_set_updated_at on public.notes;
create trigger trg_set_updated_at before update on public.notes
  for each row execute function public.set_updated_at();

drop trigger if exists trg_set_updated_at on public.custom_reminders;
create trigger trg_set_updated_at before update on public.custom_reminders
  for each row execute function public.set_updated_at();

drop trigger if exists trg_set_updated_at on public.notification_settings;
create trigger trg_set_updated_at before update on public.notification_settings
  for each row execute function public.set_updated_at();

-- =============================================================================
-- 4. Lock down enum-shaped text columns
--    Values are LOWERCASE, matching what the app serializes:
--      - custom_reminders.recurrence:  draft.recurrence.name.lowercase()
--      - profiles.default_frequency:   written as lower(payload->>'reminderFrequency')
--    (user_reminder_preferences.reminder_frequency stored UPPERCASE, but that
--    table is migrated+dropped below; profiles.default_frequency is normalized
--    lowercase by the RPC.)
-- =============================================================================

alter table public.custom_reminders
  drop constraint if exists custom_reminders_recurrence_check;
alter table public.custom_reminders
  add constraint custom_reminders_recurrence_check
  check (recurrence in ('none','daily','weekly','biweekly','monthly','semiannually','annually'));

alter table public.profiles
  drop constraint if exists profiles_onboarding_step_check;
alter table public.profiles
  add constraint profiles_onboarding_step_check
  check (onboarding_step between 0 and 9);
-- NOTE: profiles_default_frequency_check is added in section 5, AFTER the
-- default_frequency column is created (Postgres 42703 otherwise).

-- =============================================================================
-- 5. profiles — add ERD-aligned columns (write path added in section 12 RPC)
--    ERD PROFILES: full_name, phone, default_frequency, default_reminder_time,
--    subscription_tier. `phone` intentionally omitted — auth moved to email.
-- =============================================================================

alter table public.profiles
  add column if not exists full_name text;
alter table public.profiles
  add column if not exists default_frequency text;
alter table public.profiles
  add column if not exists default_reminder_time time;
alter table public.profiles
  add column if not exists subscription_tier text;

alter table public.profiles
  drop constraint if exists profiles_default_frequency_check;
alter table public.profiles
  add constraint profiles_default_frequency_check
  check (default_frequency is null or default_frequency in
         ('none','daily','weekly','biweekly','monthly','semiannually','annually'));

update public.profiles
   set full_name = coalesce(full_name, display_name)
 where full_name is null;

-- =============================================================================
-- 6. Migrate user_reminder_preferences -> profiles, then drop the old table
--    Avoids two tables claiming to be the source of truth for reminder cadence.
--    Guarded with to_regclass so the section is re-run safe.
-- =============================================================================

do $$
begin
  if to_regclass('public.user_reminder_preferences') is not null then
    update public.profiles p
       set default_frequency     = coalesce(p.default_frequency, lower(nullif(p2.reminder_frequency, ''))),
           default_reminder_time = coalesce(
             p.default_reminder_time,
             case when p2.reminder_hour is null or p2.reminder_minute is null then null
                  else make_time(p2.reminder_hour, p2.reminder_minute, 0) end
           )
      from public.user_reminder_preferences p2
     where p2.user_id = p.id;
  end if;
end $$;

drop table if exists public.user_reminder_preferences;

-- =============================================================================
-- 7. contacts — the relationship tracker's hub entity
-- =============================================================================

create table if not exists public.contacts (
  id                  uuid not null default gen_random_uuid(),
  owner_id            uuid not null,
  name                text not null,
  avatar_color        text,
  check_in_frequency  text not null default 'none',
  reminder_time       time,
  next_check_in_date  date,
  last_check_in_date  date,
  streak_count        integer not null default 0,
  created_at          timestamptz not null default now(),
  updated_at          timestamptz not null default now(),
  constraint contacts_pkey primary key (id),
  constraint contacts_owner_id_fkey foreign key (owner_id)
    references auth.users (id) on delete cascade,
  constraint contacts_check_in_frequency_check
    check (check_in_frequency in ('none','daily','weekly','biweekly','monthly'))
);

create index if not exists idx_contacts_owner         on public.contacts(owner_id);
create index if not exists idx_contacts_next_check_in on public.contacts(next_check_in_date);

alter table public.contacts enable row level security;
create policy "contacts_select_own" on public.contacts for select using (auth.uid() = owner_id);
create policy "contacts_insert_own" on public.contacts for insert with check (auth.uid() = owner_id);
create policy "contacts_update_own" on public.contacts for update
  using (auth.uid() = owner_id) with check (auth.uid() = owner_id);
create policy "contacts_delete_own" on public.contacts for delete using (auth.uid() = owner_id);

drop trigger if exists trg_set_updated_at on public.contacts;
create trigger trg_set_updated_at before update on public.contacts
  for each row execute function public.set_updated_at();

-- Backfill the contact collected by onboarding for accounts that completed
-- before the onboarding RPC started inserting into contacts.
insert into public.contacts (owner_id, name, avatar_color)
select
  p.id,
  p.contact_name,
  case p.selected_avatar_id
    when '0' then '#FFCC33'
    when '1' then '#34C759'
    when '2' then '#FF9500'
    when '3' then '#FF3B30'
    when '4' then '#AF52DE'
    when '5' then '#007AFF'
    else null
  end
from public.profiles p
where p.onboarding_completed_at is not null
  and nullif(p.contact_name, '') is not null
  and not exists (
    select 1 from public.contacts c where c.owner_id = p.id
  );

-- =============================================================================
-- 8. Wire existing content to contacts (nullable for now so existing rows
--    don't break). TODO: decide if global (no-contact) notes/reminders stay a
--    supported concept, or backfill and make these NOT NULL later.
-- =============================================================================

alter table public.notes
  add column if not exists contact_id uuid references public.contacts(id) on delete cascade;
create index if not exists idx_notes_contact on public.notes(contact_id);

alter table public.custom_reminders
  add column if not exists contact_id uuid references public.contacts(id) on delete cascade;
create index if not exists idx_custom_reminders_contact on public.custom_reminders(contact_id);

-- =============================================================================
-- 9. contact_groups — many-to-many contacts <-> groups
-- =============================================================================

create table if not exists public.contact_groups (
  contact_id uuid not null,
  group_id   uuid not null,
  constraint contact_groups_pkey primary key (contact_id, group_id),
  constraint contact_groups_contact_id_fkey foreign key (contact_id)
    references public.contacts(id) on delete cascade,
  constraint contact_groups_group_id_fkey foreign key (group_id)
    references public.groups(id) on delete cascade
);

create index if not exists idx_contact_groups_group on public.contact_groups(group_id);

alter table public.contact_groups enable row level security;
-- Checks BOTH the contact and the group belong to the caller, so you can't
-- link your contact to someone else's group id (or vice versa).
create policy "contact_groups_select_own" on public.contact_groups for select
  using (
    exists (select 1 from public.contacts c where c.id = contact_id and c.owner_id = auth.uid())
  );
create policy "contact_groups_insert_own" on public.contact_groups for insert
  with check (
    exists (select 1 from public.contacts c where c.id = contact_id and c.owner_id = auth.uid())
    and exists (select 1 from public.groups g where g.id = group_id and g.owner_id = auth.uid())
  );
create policy "contact_groups_delete_own" on public.contact_groups for delete
  using (
    exists (select 1 from public.contacts c where c.id = contact_id and c.owner_id = auth.uid())
  );

-- =============================================================================
-- 10. check_ins — the actual event log.
--     Matches the ERD exactly (no denormalized owner column); RLS resolves
--     ownership through the contact via a PK-indexed subquery.
--     contacts.streak_count / last_check_in_date / next_check_in_date are
--     denormalized caches — TODO: update them from the app (or add a trigger)
--     on every check_ins insert, or they'll drift.
-- =============================================================================

create table if not exists public.check_ins (
  id            uuid not null default gen_random_uuid(),
  contact_id    uuid not null,
  checked_in_at timestamptz not null default now(),
  note          text,
  constraint check_ins_pkey primary key (id),
  constraint check_ins_contact_id_fkey foreign key (contact_id)
    references public.contacts(id) on delete cascade
);

create index if not exists idx_check_ins_contact on public.check_ins(contact_id, checked_in_at desc);

alter table public.check_ins enable row level security;
create policy "check_ins_select_own" on public.check_ins for select
  using (
    exists (select 1 from public.contacts c where c.id = contact_id and c.owner_id = auth.uid())
  );
create policy "check_ins_insert_own" on public.check_ins for insert
  with check (
    exists (select 1 from public.contacts c where c.id = contact_id and c.owner_id = auth.uid())
  );
create policy "check_ins_delete_own" on public.check_ins for delete
  using (
    exists (select 1 from public.contacts c where c.id = contact_id and c.owner_id = auth.uid())
  );

-- =============================================================================
-- 11. brainstorm_sessions / brainstorm_topics — contact-level brainstorming
--     NOTE: brainstorm_topics ownership is a two-hop RLS join
--     (topic -> session -> contact -> owner). Test this policy once live; if
--     AI-generated topics become high-volume, consider denormalizing owner_id
--     here (unlike check_ins, where the single subquery is cheap enough).
-- =============================================================================

create table if not exists public.brainstorm_sessions (
  id          uuid not null default gen_random_uuid(),
  contact_id  uuid not null,
  created_at  timestamptz not null default now(),
  constraint brainstorm_sessions_pkey primary key (id),
  constraint brainstorm_sessions_contact_id_fkey foreign key (contact_id)
    references public.contacts(id) on delete cascade
);

create index if not exists idx_brainstorm_sessions_contact on public.brainstorm_sessions(contact_id);

alter table public.brainstorm_sessions enable row level security;
create policy "brainstorm_sessions_select_own" on public.brainstorm_sessions for select
  using (
    exists (select 1 from public.contacts c where c.id = contact_id and c.owner_id = auth.uid())
  );
create policy "brainstorm_sessions_insert_own" on public.brainstorm_sessions for insert
  with check (
    exists (select 1 from public.contacts c where c.id = contact_id and c.owner_id = auth.uid())
  );
create policy "brainstorm_sessions_delete_own" on public.brainstorm_sessions for delete
  using (
    exists (select 1 from public.contacts c where c.id = contact_id and c.owner_id = auth.uid())
  );

create table if not exists public.brainstorm_topics (
  id          uuid not null default gen_random_uuid(),
  session_id  uuid not null,
  icon        text,
  title       text not null,
  description text,
  constraint brainstorm_topics_pkey primary key (id),
  constraint brainstorm_topics_session_id_fkey foreign key (session_id)
    references public.brainstorm_sessions(id) on delete cascade
);

create index if not exists idx_brainstorm_topics_session on public.brainstorm_topics(session_id);

alter table public.brainstorm_topics enable row level security;
create policy "brainstorm_topics_select_own" on public.brainstorm_topics for select
  using (
    exists (
      select 1
      from public.brainstorm_sessions s
      join public.contacts c on c.id = s.contact_id
      where s.id = session_id and c.owner_id = auth.uid()
    )
  );
create policy "brainstorm_topics_insert_own" on public.brainstorm_topics for insert
  with check (
    exists (
      select 1
      from public.brainstorm_sessions s
      join public.contacts c on c.id = s.contact_id
      where s.id = session_id and c.owner_id = auth.uid()
    )
  );
create policy "brainstorm_topics_delete_own" on public.brainstorm_topics for delete
  using (
    exists (
      select 1
      from public.brainstorm_sessions s
      join public.contacts c on c.id = s.contact_id
      where s.id = session_id and c.owner_id = auth.uid()
    )
  );

-- =============================================================================
-- 12. badges / user_badges — gamification (public catalog + per-user unlocks)
-- =============================================================================

create table if not exists public.badges (
  id         uuid not null default gen_random_uuid(),
  name       text not null,
  threshold  integer,
  constraint badges_pkey primary key (id)
);

alter table public.badges enable row level security;
-- Badges are a public catalog: any authenticated user can read. Write/delete
-- are intentionally left without policies (admin/service-role only).
create policy "badges_select_all_authenticated" on public.badges for select
  using (auth.role() = 'authenticated');

create table if not exists public.user_badges (
  owner_id     uuid not null,
  badge_id     uuid not null,
  unlocked_at  timestamptz not null default now(),
  constraint user_badges_pkey primary key (owner_id, badge_id),
  constraint user_badges_owner_id_fkey foreign key (owner_id)
    references auth.users(id) on delete cascade,
  constraint user_badges_badge_id_fkey foreign key (badge_id)
    references public.badges(id) on delete cascade
);

create index if not exists idx_user_badges_badge on public.user_badges(badge_id);

alter table public.user_badges enable row level security;
create policy "user_badges_select_own" on public.user_badges for select using (auth.uid() = owner_id);
create policy "user_badges_insert_own" on public.user_badges for insert with check (auth.uid() = owner_id);
create policy "user_badges_delete_own" on public.user_badges for delete using (auth.uid() = owner_id);

-- =============================================================================
-- 13. subscriptions + subscription_tier sync
--     profiles.subscription_tier is a fast-read cache of the active plan;
--     keep it in sync whenever subscriptions.plan changes.
-- =============================================================================

create table if not exists public.subscriptions (
  id             uuid not null default gen_random_uuid(),
  owner_id       uuid not null,
  plan           text,
  status         text,
  trial_ends_at  timestamptz,
  created_at     timestamptz not null default now(),
  updated_at     timestamptz not null default now(),
  constraint subscriptions_pkey primary key (id),
  constraint subscriptions_owner_id_fkey foreign key (owner_id)
    references auth.users(id) on delete cascade
);

create index if not exists idx_subscriptions_owner on public.subscriptions(owner_id);

alter table public.subscriptions enable row level security;
create policy "subscriptions_select_own" on public.subscriptions for select using (auth.uid() = owner_id);
create policy "subscriptions_insert_own" on public.subscriptions for insert with check (auth.uid() = owner_id);
create policy "subscriptions_update_own" on public.subscriptions for update
  using (auth.uid() = owner_id) with check (auth.uid() = owner_id);
create policy "subscriptions_delete_own" on public.subscriptions for delete using (auth.uid() = owner_id);

drop trigger if exists trg_set_updated_at on public.subscriptions;
create trigger trg_set_updated_at before update on public.subscriptions
  for each row execute function public.set_updated_at();

create or replace function public.sync_subscription_tier()
returns trigger
language plpgsql
as $$
declare
  v_tier text;
begin
  -- "what the user currently has access to": a non-active subscription maps to
  -- the free tier regardless of the plan text. Adjust the active-set here as
  -- needed (e.g. add 'trialing').
  v_tier := case
    when new.status in ('active', 'trialing') then new.plan
    else 'free'
  end;
  update public.profiles set subscription_tier = v_tier where id = new.owner_id;
  return new;
end;
$$;

drop trigger if exists trg_sync_subscription_tier on public.subscriptions;
create trigger trg_sync_subscription_tier
  after insert or update of plan, status on public.subscriptions
  for each row execute function public.sync_subscription_tier();

-- =============================================================================
-- 14. complete_onboarding RPC replacement
--     Now writes the ERD-aligned profiles columns (full_name,
--     default_frequency, default_reminder_time) so they don't go stale,
--     writes groups/notes/custom_reminders with owner_id, and no longer
--     references user_reminder_preferences (dropped above).
--
--     NON-DESTRUCTIVE: seeding is a no-op for accounts that already finished
--     onboarding, and even for first-timers it only inserts when the table is
--     empty (no blanket delete). Re-invoking the RPC can never wipe real data.
--
--     PAYLOAD KEYS: verified against OnboardingDtos.kt /
--     SupabaseOnboardingProfileDataSource.submitOnboarding, not guessed.
--       - reminderTime is FLAT, not nested: the domain ReminderTimeDraft{hour,
--         minute} is flattened to top-level reminderHour/reminderMinute in the
--         DTO, so payload->>'reminderHour' / ->'reminderMinute' are correct.
--       - reminderFrequency arrives UPPERCASE (enum .name) and is lowercased
--         here to satisfy profiles_default_frequency_check.
--       - customReminders[].recurrence already arrives lowercase.
--       - customReminders[]/notes[] have NO contactId key today (DTOs don't
--         send one); the left join below handles both null and ownership.
--
--     SECURITY: the function runs security definer (RLS is bypassed inside),
--     so any contactId in the payload is validated against the caller's own
--     contacts via the left join — a crafted id pointing at someone else's
--     contact silently becomes NULL instead of being accepted.
-- =============================================================================

create or replace function complete_onboarding(payload jsonb)
returns void
language plpgsql
security definer
as $$
declare
  v_user_id uuid;
  v_time time;
  v_already_complete boolean;
  v_contact_id uuid;
  v_selected_group_id uuid;
begin
  v_user_id := auth.uid();
  if v_user_id is null then
    raise exception 'Not authenticated';
  end if;

  -- Defense in depth: if this account already finished onboarding, do NOT touch
  -- its seeded data. The app routes returning users straight to Home, but if
  -- this RPC is ever re-invoked (e.g. a stale draft from a reinstall), it must
  -- be a no-op for existing data rather than wiping it.
  select exists(
    select 1 from public.profiles where id = v_user_id and onboarding_completed_at is not null
  ) into v_already_complete;

  v_time := case
    when payload->>'reminderHour' is null or payload->>'reminderMinute' is null then null
    else make_time((payload->>'reminderHour')::int, (payload->>'reminderMinute')::int, 0)
  end;

  -- upsert profile (safe regardless of v_already_complete — the columns below
  -- only get set when the RPC actually runs)
  insert into profiles (id, email, email_verified, display_name, full_name, contact_name,
                        avatar_url, selected_avatar_id, default_frequency, default_reminder_time,
                        subscription_tier, onboarding_step, onboarding_completed_at, updated_at)
  values (
    v_user_id,
    payload->>'email',
    coalesce((payload->>'emailVerified')::boolean, false),
    payload->>'displayName',
    coalesce(payload->>'displayName', payload->>'fullName'),
    payload->>'contactName',
    payload->>'avatarUrl',
    payload->>'selectedAvatarId',
    lower(payload->>'reminderFrequency'),
    v_time,
    null,
    9,
    now(),
    now()
  )
  on conflict (id) do update set
    display_name          = excluded.display_name,
    full_name             = excluded.full_name,
    contact_name          = excluded.contact_name,
    avatar_url            = excluded.avatar_url,
    selected_avatar_id    = excluded.selected_avatar_id,
    default_frequency     = excluded.default_frequency,
    default_reminder_time = excluded.default_reminder_time,
    onboarding_step       = 9,
    onboarding_completed_at = now(),
    updated_at            = now();

  -- Non-destructive seeding. The built-in groups are ensured for every account
  -- so users who completed onboarding before they were persisted are repaired.
  -- For a first-time user, onboarding-created rows are inserted without
  -- deleting or replacing real data.
  insert into public.groups (owner_id, name, color)
  select v_user_id, starter.name, null
    from (values ('Family'::text), ('Friends'::text)) as starter(name)
   where not exists (
     select 1
       from public.groups existing
      where existing.owner_id = v_user_id
        and lower(existing.name) = lower(starter.name)
   );

  if not v_already_complete then
    -- Custom groups are seeded once, while built-in groups are handled above.
    insert into public.groups (owner_id, name, color)
    select v_user_id, g->>'name', g->>'color'
      from jsonb_array_elements(coalesce(payload->'groups', '[]'::jsonb)) as g
     where nullif(trim(g->>'name'), '') is not null
       and lower(trim(g->>'name')) not in ('family', 'friends')
       and not exists (
         select 1
           from public.groups existing
          where existing.owner_id = v_user_id
            and lower(existing.name) = lower(trim(g->>'name'))
       );

    -- The onboarding contact is the first Home contact. Keep this seed
    -- idempotent and retain the selected avatar as the Home avatar color.
    if nullif(payload->>'contactName', '') is not null
       and not exists (select 1 from public.contacts where owner_id = v_user_id) then
      insert into public.contacts (owner_id, name, avatar_color)
      values (v_user_id, payload->>'contactName', payload->>'selectedAvatarColor')
      returning id into v_contact_id;
    end if;

    if v_contact_id is not null and nullif(payload->>'selectedGroupName', '') is not null then
      select id into v_selected_group_id
        from public.groups
       where owner_id = v_user_id
         and lower(name) = lower(payload->>'selectedGroupName')
       order by created_at
       limit 1;

      if v_selected_group_id is not null then
        insert into public.contact_groups (contact_id, group_id)
        values (v_contact_id, v_selected_group_id)
        on conflict (contact_id, group_id) do nothing;
      end if;
    end if;

    -- custom reminders: only seed if the account has none yet
    if not exists (select 1 from public.custom_reminders where owner_id = v_user_id) then
      insert into custom_reminders (owner_id, title, description, recurrence, date_epoch_millis, contact_id)
      select v_user_id, r->>'title', r->>'description', r->>'recurrence',
             (r->>'dateEpochMillis')::bigint, c.id
      from jsonb_array_elements(payload->'customReminders') as r
      left join public.contacts c
        on c.id = (r->>'contactId')::uuid and c.owner_id = v_user_id;
    end if;

    -- notes: only seed if the account has none yet
    if not exists (select 1 from public.notes where owner_id = v_user_id) then
      insert into notes (owner_id, title, body, contact_id)
      select v_user_id, n->>'title', n->>'body', c.id
      from jsonb_array_elements(payload->'notes') as n
      left join public.contacts c
        on c.id = (n->>'contactId')::uuid and c.owner_id = v_user_id;
    end if;
  end if;

  -- upsert notification settings
  insert into notification_settings (user_id, permission_asked, permission_granted, updated_at)
  values (
    v_user_id,
    coalesce((payload->>'notificationPermissionAsked')::boolean, false),
    coalesce((payload->>'notificationPermissionGranted')::boolean, false),
    now()
  )
  on conflict (user_id) do update set
    permission_asked    = excluded.permission_asked,
    permission_granted  = excluded.permission_granted,
    updated_at          = now();
end;
$$;
