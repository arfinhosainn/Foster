-- =============================================================================
-- Foster Onboarding Schema
-- Run this in the Supabase SQL Editor.
-- =============================================================================

-- 1. TABLES -------------------------------------------------------------------

create table if not exists profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  email text,
  email_verified boolean not null default false,
  display_name text,
  contact_name text,
  avatar_url text,
  selected_avatar_id text,
  onboarding_step integer not null default 0,
  onboarding_completed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists groups (
  id uuid primary key default gen_random_uuid(),
  owner_user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  color text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists user_reminder_preferences (
  user_id uuid primary key references auth.users(id) on delete cascade,
  reminder_frequency text,
  reminder_hour integer,
  reminder_minute integer,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists custom_reminders (
  id uuid primary key default gen_random_uuid(),
  owner_user_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  description text not null default '',
  recurrence text not null default 'none',
  date_epoch_millis bigint,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists notes (
  id uuid primary key default gen_random_uuid(),
  owner_user_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  body text not null default '',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists notification_settings (
  user_id uuid primary key references auth.users(id) on delete cascade,
  permission_asked boolean not null default false,
  permission_granted boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- 2. ROW LEVEL SECURITY -------------------------------------------------------

alter table profiles enable row level security;
alter table groups enable row level security;
alter table user_reminder_preferences enable row level security;
alter table custom_reminders enable row level security;
alter table notes enable row level security;
alter table notification_settings enable row level security;

-- Profiles (keyed on auth.uid)
create policy "Users can read own profile"
  on profiles for select using (auth.uid() = id);
create policy "Users can upsert own profile"
  on profiles for insert with check (auth.uid() = id);
create policy "Users can update own profile"
  on profiles for update using (auth.uid() = id) with check (auth.uid() = id);

-- Groups (keyed on owner_user_id)
create policy "Users can read own groups"
  on groups for select using (auth.uid() = owner_user_id);
create policy "Users can insert own groups"
  on groups for insert with check (auth.uid() = owner_user_id);
create policy "Users can update own groups"
  on groups for update using (auth.uid() = owner_user_id) with check (auth.uid() = owner_user_id);
create policy "Users can delete own groups"
  on groups for delete using (auth.uid() = owner_user_id);

-- user_reminder_preferences (keyed on user_id)
create policy "Users can read own reminder preferences"
  on user_reminder_preferences for select using (auth.uid() = user_id);
create policy "Users can upsert own reminder preferences"
  on user_reminder_preferences for insert with check (auth.uid() = user_id);
create policy "Users can update own reminder preferences"
  on user_reminder_preferences for update using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- custom_reminders (keyed on owner_user_id)
create policy "Users can read own custom reminders"
  on custom_reminders for select using (auth.uid() = owner_user_id);
create policy "Users can insert own custom reminders"
  on custom_reminders for insert with check (auth.uid() = owner_user_id);
create policy "Users can update own custom reminders"
  on custom_reminders for update using (auth.uid() = owner_user_id) with check (auth.uid() = owner_user_id);
create policy "Users can delete own custom reminders"
  on custom_reminders for delete using (auth.uid() = owner_user_id);

-- notes (keyed on owner_user_id)
create policy "Users can read own notes"
  on notes for select using (auth.uid() = owner_user_id);
create policy "Users can insert own notes"
  on notes for insert with check (auth.uid() = owner_user_id);
create policy "Users can update own notes"
  on notes for update using (auth.uid() = owner_user_id) with check (auth.uid() = owner_user_id);
create policy "Users can delete own notes"
  on notes for delete using (auth.uid() = owner_user_id);

-- notification_settings (keyed on user_id)
create policy "Users can read own notification settings"
  on notification_settings for select using (auth.uid() = user_id);
create policy "Users can upsert own notification settings"
  on notification_settings for insert with check (auth.uid() = user_id);
create policy "Users can update own notification settings"
  on notification_settings for update using (auth.uid() = user_id) with check (auth.uid() = user_id);

-- 3. RPC FUNCTION -------------------------------------------------------------

create or replace function complete_onboarding(payload jsonb)
returns void
language plpgsql
security definer
as $$
declare
  v_user_id uuid;
begin
  v_user_id := auth.uid();
  if v_user_id is null then
    raise exception 'Not authenticated';
  end if;

  -- upsert profile
  insert into profiles (id, email, email_verified, display_name, contact_name, avatar_url,
                        selected_avatar_id, onboarding_step, onboarding_completed_at, updated_at)
  values (
    v_user_id,
    payload->>'email',
    coalesce((payload->>'emailVerified')::boolean, false),
    payload->>'displayName',
    payload->>'contactName',
    payload->>'avatarUrl',
    payload->>'selectedAvatarId',
    9,
    now(),
    now()
  )
  on conflict (id) do update set
    display_name          = excluded.display_name,
    contact_name          = excluded.contact_name,
    avatar_url            = excluded.avatar_url,
    selected_avatar_id    = excluded.selected_avatar_id,
    onboarding_step       = 9,
    onboarding_completed_at = now(),
    updated_at            = now();

  -- replace groups
  delete from groups where owner_user_id = v_user_id;
  insert into groups (owner_user_id, name, color)
  select v_user_id, g->>'name', g->>'color'
  from jsonb_array_elements(payload->'groups') as g;

  -- Family and Friends are built-in onboarding groups and must be available
  -- on Home even when an older client sends no groups in its payload.
  insert into groups (owner_user_id, name)
  select v_user_id, starter.name
  from (values ('Family'::text), ('Friends'::text)) as starter(name)
  where not exists (
    select 1
    from groups existing
    where existing.owner_user_id = v_user_id
      and lower(existing.name) = lower(starter.name)
  );

  -- upsert reminder preferences
  insert into user_reminder_preferences (user_id, reminder_frequency, reminder_hour, reminder_minute, updated_at)
  values (
    v_user_id,
    payload->>'reminderFrequency',
    (payload->>'reminderHour')::integer,
    (payload->>'reminderMinute')::integer,
    now()
  )
  on conflict (user_id) do update set
    reminder_frequency  = excluded.reminder_frequency,
    reminder_hour       = excluded.reminder_hour,
    reminder_minute     = excluded.reminder_minute,
    updated_at          = now();

  -- replace custom reminders
  delete from custom_reminders where owner_user_id = v_user_id;
  insert into custom_reminders (owner_user_id, title, description, recurrence, date_epoch_millis)
  select v_user_id, r->>'title', r->>'description', r->>'recurrence', (r->>'dateEpochMillis')::bigint
  from jsonb_array_elements(payload->'customReminders') as r;

  -- replace notes
  delete from notes where owner_user_id = v_user_id;
  insert into notes (owner_user_id, title, body)
  select v_user_id, n->>'title', n->>'body'
  from jsonb_array_elements(payload->'notes') as n;

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
