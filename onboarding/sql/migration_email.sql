-- Migration: phone → email

-- Add new columns
alter table profiles add column if not exists email text;
alter table profiles add column if not exists email_verified boolean not null default false;

-- Remove old phone columns
alter table profiles drop column if exists phone_number;
alter table profiles drop column if exists phone_verified;

-- Update RPC function
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
  insert into profiles (id, email, email_verified, display_name, contact_name,
                        selected_avatar_id, onboarding_step, onboarding_completed_at, updated_at)
  values (
    v_user_id,
    payload->>'email',
    coalesce((payload->>'emailVerified')::boolean, false),
    payload->>'displayName',
    payload->>'contactName',
    payload->>'selectedAvatarId',
    11,
    now(),
    now()
  )
  on conflict (id) do update set
    email                 = excluded.email,
    email_verified        = excluded.email_verified,
    display_name          = excluded.display_name,
    contact_name          = excluded.contact_name,
    selected_avatar_id    = excluded.selected_avatar_id,
    onboarding_step       = 11,
    onboarding_completed_at = now(),
    updated_at            = now();

  -- replace groups
  delete from groups where owner_user_id = v_user_id;
  insert into groups (owner_user_id, name, color)
  select v_user_id, g->>'name', g->>'color'
  from jsonb_array_elements(payload->'groups') as g;

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
