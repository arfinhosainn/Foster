-- =============================================================================
-- Repair onboarding contact details
--
-- Apply after migration_v2_erd.sql. The original v2 RPC created the onboarding
-- contact without its schedule and attempted to derive contact_id from payload
-- fields that the onboarding DTO does not send. This migration repairs existing
-- rows and replaces the RPC for future onboarding submissions.
-- =============================================================================

alter table public.contacts
  drop constraint if exists contacts_check_in_frequency_check;
alter table public.contacts
  add constraint contacts_check_in_frequency_check
  check (check_in_frequency in ('none','daily','weekly','biweekly','monthly','semiannually','annually'));

-- Backfill the onboarding contact's schedule when the contact was created by
-- the old RPC before cadence and reminder time were persisted on contacts.
update public.contacts c
   set check_in_frequency = coalesce(nullif(lower(p.default_frequency), ''), 'none'),
       reminder_time = p.default_reminder_time
  from public.profiles p
 where c.owner_id = p.id
   and c.id = (
     select first_contact.id
       from public.contacts first_contact
      where first_contact.owner_id = p.id
      order by first_contact.created_at
      limit 1
   )
   and p.default_frequency is not null;

-- Existing onboarding content was account-owned but had no contact_id. Attach
-- it to the user's first contact, which is the contact created by onboarding.
update public.notes n
   set contact_id = c.id
  from (
    select distinct on (owner_id) id, owner_id
      from public.contacts
     order by owner_id, created_at
  ) c
 where n.owner_id = c.owner_id
   and n.contact_id is null;

update public.custom_reminders r
   set contact_id = c.id
  from (
    select distinct on (owner_id) id, owner_id
      from public.contacts
     order by owner_id, created_at
  ) c
 where r.owner_id = c.owner_id
   and r.contact_id is null;

create or replace function public.complete_onboarding(payload jsonb)
returns void
language plpgsql
security definer
set search_path = public
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

  select exists(
    select 1
      from public.profiles
     where id = v_user_id
       and onboarding_completed_at is not null
  ) into v_already_complete;

  v_time := case
    when payload->>'reminderHour' is null or payload->>'reminderMinute' is null then null
    else make_time((payload->>'reminderHour')::int, (payload->>'reminderMinute')::int, 0)
  end;

  insert into public.profiles (
    id, email, email_verified, display_name, full_name, contact_name,
    avatar_url, selected_avatar_id, default_frequency, default_reminder_time,
    subscription_tier, onboarding_step, onboarding_completed_at, updated_at
  )
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
    display_name = excluded.display_name,
    full_name = excluded.full_name,
    contact_name = excluded.contact_name,
    avatar_url = excluded.avatar_url,
    selected_avatar_id = excluded.selected_avatar_id,
    default_frequency = excluded.default_frequency,
    default_reminder_time = excluded.default_reminder_time,
    onboarding_step = 9,
    onboarding_completed_at = now(),
    updated_at = now();

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

    select id into v_contact_id
      from public.contacts
     where owner_id = v_user_id
     order by created_at
     limit 1;

    if v_contact_id is null and nullif(payload->>'contactName', '') is not null then
      insert into public.contacts (
        owner_id, name, avatar_color, check_in_frequency, reminder_time
      )
      values (
        v_user_id,
        payload->>'contactName',
        payload->>'selectedAvatarColor',
        coalesce(nullif(lower(payload->>'reminderFrequency'), ''), 'none'),
        v_time
      )
      returning id into v_contact_id;
    end if;

    -- Always sync the onboarding contact's day-reminder cadence from the payload,
    -- even when the contact pre-existed (a resumed draft). Previously this only ran
    -- on insert, so a contact seeded earlier kept check_in_frequency = 'none' and
    -- silently ignored the Daily / Weekly / ... pick.
    if v_contact_id is not null then
      update public.contacts
         set check_in_frequency = coalesce(nullif(lower(payload->>'reminderFrequency'), ''), 'none'),
             reminder_time      = v_time,
             updated_at         = now()
       where id = v_contact_id
         and owner_id = v_user_id;
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

    if v_contact_id is not null
       and not exists (select 1 from public.custom_reminders where owner_id = v_user_id) then
      insert into public.custom_reminders (
        owner_id, title, description, recurrence, date_epoch_millis, contact_id
      )
      select v_user_id, r->>'title', r->>'description', r->>'recurrence',
             (r->>'dateEpochMillis')::bigint, v_contact_id
        from jsonb_array_elements(coalesce(payload->'customReminders', '[]'::jsonb)) as r;
    end if;

    if v_contact_id is not null
       and not exists (select 1 from public.notes where owner_id = v_user_id) then
      insert into public.notes (owner_id, title, body, contact_id)
      select v_user_id, n->>'title', n->>'body', v_contact_id
        from jsonb_array_elements(coalesce(payload->'notes', '[]'::jsonb)) as n;
    end if;
  end if;

  insert into public.notification_settings (
    user_id, permission_asked, permission_granted, updated_at
  )
  values (
    v_user_id,
    coalesce((payload->>'notificationPermissionAsked')::boolean, false),
    coalesce((payload->>'notificationPermissionGranted')::boolean, false),
    now()
  )
  on conflict (user_id) do update set
    permission_asked = excluded.permission_asked,
    permission_granted = excluded.permission_granted,
    updated_at = now();
end;
$$;

grant execute on function public.complete_onboarding(jsonb) to authenticated;