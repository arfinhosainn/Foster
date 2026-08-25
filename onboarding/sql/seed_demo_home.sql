-- =============================================================================
-- Nekko — DEMO DATA SEED for the Home screen
--
-- WHAT IT NEEDS FROM YOU (top of the DO block):
--   * v_user_id   : your auth.users UUID  (Dashboard -> Authentication -> Users)
--       OR
--   * v_user_email: the email of that account (looked up in auth.users)
--   Sign up / create that account FIRST (in the app or via dashboard invite),
--   because contacts.owner_id has a foreign key to auth.users(id).
--
-- WHAT IT CREATES (8 contacts covering every Home state):
--   1 Maya Patel    daily      DUE TODAY            streak 12
--   2 Leo Alvarez   daily      CHECKED-IN TODAY     streak 34
--   3 Grandma Rose  weekly     OVERDUE 10 days      (missed day(s) visible)
--   4 Sam Okafor    biweekly   upcoming in 5 days
--   5 Aisha Rahman  monthly    due tomorrow         streak 7
--   6 Daniel Kim    weekly     OVERDUE 3 days       second missed example
--   7 Nina Rossi    none       unscheduled          streak 0
--   8 Tomás Rivera  annually   due in ~4 months     streak 52
-- plus groups, group links, notes, custom reminders, check-in history,
-- missed_check_ins rows, and the badge catalog (auto-unlock fires on inserts).
--
-- WARNING: DESTRUCTIVE for the target account — it wipes ALL data owned by
-- that user (contacts/groups/notes/reminders/history) before seeding.
-- Idempotent otherwise: safe to run repeatedly, always resets to this state.
-- =============================================================================

do $$
declare
  -- >>> FILL ONE OF THESE TWO <<<
  v_user_id    uuid := 'eee0b312-9afa-49c7-8308-2a303e52b74c';                 -- e.g. '11111111-2222-3333-4444-555555555555'
  v_user_email text := 'arfinhosain03@gmail.com';    -- resolved against auth.users

  v_owner      uuid;
  v_badge_soil uuid;
  c1 uuid; c2 uuid; c3 uuid; c4 uuid; c5 uuid; c6 uuid; c7 uuid; c8 uuid;
  g_fam uuid; g_friends uuid; g_gym uuid; g_work uuid;
begin
  -- ---------------------------------------------------------------------------
  -- Resolve the owner
  -- ---------------------------------------------------------------------------
  if v_user_id is not null then
    v_owner := v_user_id;
  else
    select u.id into v_owner
      from auth.users u
     where lower(u.email) = lower(v_user_email)
     limit 1;
  end if;

  if v_owner is null then
    raise exception 'No auth.users row found. Sign up first, then set v_user_id/v_user_email.';
  end if;
  raise notice 'Seeding demo data for user %', v_owner;

  -- Profile must look "onboarding complete" so the app routes to Home.
  insert into public.profiles (id, email, onboarding_step, onboarding_completed_at)
  values (v_owner, coalesce((select email from auth.users where id = v_owner), 'demo@local'),
          9, now())
  on conflict (id) do update set
    onboarding_step         = 9,
    onboarding_completed_at = coalesce(public.profiles.onboarding_completed_at, now()),
    updated_at              = now();

  -- ---------------------------------------------------------------------------
  -- Clean slate for THIS user (contacts cascade their check-ins / misses /
  -- notes / reminders / group links)
  -- ---------------------------------------------------------------------------
  delete from public.check_ins ci using public.contacts c
    where ci.contact_id = c.id and c.owner_id = v_owner;
  delete from public.missed_check_ins m using public.contacts c
    where m.contact_id = c.id and c.owner_id = v_owner;
  delete from public.notes           where owner_id = v_owner;
  delete from public.custom_reminders where owner_id = v_owner;
  delete from public.contact_groups cg using public.contacts c
    where cg.contact_id = c.id and c.owner_id = v_owner;
  delete from public.contacts        where owner_id = v_owner;
  delete from public.groups          where owner_id = v_owner;
  delete from public.user_badges     where owner_id = v_owner;

  -- Badge catalog (same guarded seeds as badges.sql, kept standalone-safe).
  insert into public.badges (name, description, threshold)
  select v.name, v.description, v.threshold
    from (values
      ('Soil',         'Your very first check-in starts the soil.',        1),
      ('Lotus Flower', 'Reach 15 check-ins and your lotus flower begins to bloom.', 15),
      ('Sunflower',    'Reach 30 check-ins and grow a sunflower.',        30),
      ('Brown Flower', 'Reach 45 check-ins and grow a brown flower.',     45),
      ('Blue Flower',  'Reach 60 check-ins and grow a blue lotus.',       60),
      ('Pink Flower',  'Reach 75 check-ins and grow a pink flower.',      75),
      ('Green Flower', 'Reach 90 check-ins and grow a green flower.',     90),
      ('Mushrooms',    'Reach 115 check-ins and discover the mushrooms.',115)
    ) as v(name, description, threshold)
  where not exists (select 1 from public.badges b where b.name = v.name);

  select id into v_badge_soil from public.badges where name = 'Soil';

  -- ---------------------------------------------------------------------------
  -- Groups
  -- ---------------------------------------------------------------------------
  insert into public.groups (owner_id, name, color)
  values (v_owner, 'Family', '#AF52DE') returning id into g_fam;

  insert into public.groups (owner_id, name, color)
  values (v_owner, 'Friends', '#007AFF') returning id into g_friends;

  insert into public.groups (owner_id, name, color)
  values (v_owner, 'Gym Buddies', '#34C759') returning id into g_gym;

  insert into public.groups (owner_id, name, color)
  values (v_owner, 'Work', '#FF9500') returning id into g_work;

  -- ---------------------------------------------------------------------------
  -- Contacts — every Home state
  -- ---------------------------------------------------------------------------
  insert into public.contacts
    (owner_id, name, avatar_color, check_in_frequency, reminder_time,
     next_check_in_date, last_check_in_date, streak_count)
  values
    (v_owner, 'Maya Patel',   '#FFCC33', 'daily',      '09:00',
     current_date,             current_date - 1, 12)
    returning id into c1;

  insert into public.contacts
    (owner_id, name, avatar_color, check_in_frequency, reminder_time,
     next_check_in_date, last_check_in_date, streak_count)
  values
    (v_owner, 'Leo Alvarez',  '#34C759', 'daily',      '20:30',
     current_date + 1,         current_date,     34)
    returning id into c2;

  insert into public.contacts
    (owner_id, name, avatar_color, check_in_frequency, reminder_time,
     next_check_in_date, last_check_in_date, streak_count)
  values
    (v_owner, 'Grandma Rose', '#FF3B30', 'weekly',     '18:00',
     current_date - 10,        current_date - 38, 21)
    returning id into c3;

  insert into public.contacts
    (owner_id, name, avatar_color, check_in_frequency, reminder_time,
     next_check_in_date, last_check_in_date, streak_count)
  values
    (v_owner, 'Sam Okafor',   '#AF52DE', 'biweekly',   null,
     current_date + 5,         current_date - 9,  3)
    returning id into c4;

  insert into public.contacts
    (owner_id, name, avatar_color, check_in_frequency, reminder_time,
     next_check_in_date, last_check_in_date, streak_count)
  values
    (v_owner, 'Aisha Rahman', '#007AFF', 'monthly',    '12:00',
     current_date + 1,         current_date - 29, 7)
    returning id into c5;

  insert into public.contacts
    (owner_id, name, avatar_color, check_in_frequency, reminder_time,
     next_check_in_date, last_check_in_date, streak_count)
  values
    (v_owner, 'Daniel Kim',   '#FFCC33', 'weekly',     '17:30',
     current_date - 3,         current_date - 24, 8)
    returning id into c6;

  insert into public.contacts
    (owner_id, name, avatar_color, check_in_frequency, reminder_time,
     next_check_in_date, last_check_in_date, streak_count)
  values
    (v_owner, 'Nina Rossi',   '#FF9500', 'none',       null,
     null,                     null,              0)
    returning id into c7;

  insert into public.contacts
    (owner_id, name, avatar_color, check_in_frequency, reminder_time,
     next_check_in_date, last_check_in_date, streak_count)
  values
    (v_owner, 'Tomás Rivera', '#FF3B30', 'annually',   '10:00',
     current_date + 120,       current_date - 245, 52)
    returning id into c8;

  -- ---------------------------------------------------------------------------
  -- Group memberships
  -- ---------------------------------------------------------------------------
  insert into public.contact_groups (contact_id, group_id) values
    (c1, g_friends), (c2, g_gym), (c3, g_fam), (c5, g_fam),
    (c5, g_friends), (c6, g_work), (c8, g_fam);

  -- ---------------------------------------------------------------------------
  -- Notes
  -- ---------------------------------------------------------------------------
  insert into public.notes (owner_id, title, body, contact_id) values
    (v_owner, 'Coffee order',   'Oat milk latte, extra shot. Never forgets her book.', c1),
    (v_owner, 'Birthday ideas', 'Wants the ceramics workshop for her birthday.',        c3),
    (v_owner, 'Project intro',  'Introduced to design team — follow up next sprint.',   c4),
    (v_owner, 'Running pace',   'Training for 10k, easy pace around 6:00/km.',          c2);

  -- ---------------------------------------------------------------------------
  -- Custom reminders (recurrence: none/daily/weekly/biweekly/monthly/
  --                  semiannually/annually; epoch millis dates)
  -- ---------------------------------------------------------------------------
  insert into public.custom_reminders
    (owner_id, title, description, recurrence, date_epoch_millis, contact_id)
  values
    (v_owner, 'Send flowers',     'Anniversary of grandpa''s passing.', 'annually',
     (extract(epoch from (current_date + 14)) * 1000)::bigint, c3),
    (v_owner, 'Ask about the exam','She mentioned it twice — remember!', 'weekly',
     (extract(epoch from (current_date + 2)) * 1000)::bigint,  c1),
    (v_owner, 'Return his book',  'Lent him "Project Hail Mary".',       'none',
     (extract(epoch from (current_date + 7)) * 1000)::bigint,  c2);

  -- ---------------------------------------------------------------------------
  -- Check-in history (badge trigger unlocks Soil automatically on insert)
  -- ---------------------------------------------------------------------------
  insert into public.check_ins (contact_id, checked_in_at, note) values
    (c1, now() - interval '1 day',  'Quick catch-up call'),
    (c1, now() - interval '2 days', null),
    (c1, now() - interval '3 days', 'Walked in the park'),
    (c1, now() - interval '5 days', null),
    (c2, now(),                     'Gym session done'),
    (c2, now() - interval '1 day',  null),
    (c2, now() - interval '2 days', null),
    (c2, now() - interval '4 days', 'Leg day'),
    (c3, now() - interval '38 days','Sunday roast'),
    (c3, now() - interval '45 days',null),
    (c5, now() - interval '29 days','Monthly dinner'),
    (c8, now() - interval '245 days','His annual visit');

  -- Unlock any badge already earned by this seeded history (Soil at minimum).
  if v_badge_soil is not null then
    insert into public.user_badges (owner_id, badge_id)
    values (v_owner, v_badge_soil)
    on conflict (owner_id, badge_id) do nothing;
  end if;

  -- ---------------------------------------------------------------------------
  -- Missed check-ins (the app's sync_missed_check_ins RPC also back-fills
  -- these automatically on Home launch — pre-seeding makes them immediate)
  -- ---------------------------------------------------------------------------
  insert into public.missed_check_ins (contact_id, scheduled_date) values
    (c3, current_date - 10),
    (c3, current_date - 3),
    (c6, current_date - 3)
  on conflict (contact_id, scheduled_date) do nothing;

  raise notice 'Done. Log in as that user — Home now shows all states.';
end $$;
