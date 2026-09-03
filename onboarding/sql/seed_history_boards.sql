-- =============================================================================
-- Foster — HISTORY BOARDS SEED RUNNER
--
-- Fills the check-in history so the History screen shows the board archive:
--   3 fully-finished 26-dot boards + the current in-progress board (7 dots).
--
--   Board 3 (newest, TOP of history) : all 26 days checked in  -> ★ Perfect
--   Board 2 (middle)                 : 24 checked / 2 missed   -> gap cells
--   Board 1 (oldest)                 : 22 checked / 4 missed   -> gap cells
--   Board 4 (current)                : 7 dots filled (in progress on Home)
--
-- HOW IT WORKS
--   Boards are 26-day windows anchored to the earliest check-in. The runner
--   back-fills 84 days (anchor = today-84) so:
--     board 1 = today-84..today-59, board 2 = today-58..today-33,
--     board 3 = today-32..today-7,  board 4 = today-6..today+19 (current)
--   Single noon-UTC timestamps keep the calendar day stable across timezones.
--
-- WHAT IT DOES / DOESN'T DO
--   * Uses up to the account's first 3 existing contacts (by created_at); if
--     the account has none, creates 3 test contacts ("Maya / Leo / Sam").
--   * v_reset_history := TRUE  -> wipes ONLY this user's check_ins +
--     missed_check_ins (so the demo is deterministic and re-runnable).
--     Contacts/groups/notes/reminders are never touched.
--   * Sets each seeded contact to daily (next_check_in_date = today+1,
--     last = today, streak = 3) so Home looks coherent too.
--   * Badge triggers fire on insert exactly like real usage (harmless).
--
-- RUN IT
--   Paste into the Supabase Dashboard -> SQL Editor and Run (as postgres).
--   Then open History from the status card on Home -> 3 board cards.
-- =============================================================================

do $$
declare
  -- >>> FILL ONE OF THESE TWO <<<
  v_user_id    uuid := 'eee0b312-9afa-49c7-8308-2a303e52b74c';  -- e.g. '11111111-2222-3333-4444-555555555555'
  v_user_email text := 'arfinhosain03@gmail.com';               -- resolved against auth.users

  -- Set to FALSE to keep existing check-in history (boards will OVERLAY it,
  -- and the anchor becomes your current earliest activity).
  v_reset_history boolean := true;

  v_owner        uuid;
  v_contact_ids  uuid[] := '{}';
  v_c            uuid;
  v_anchor       date := current_date - 84;   -- earliest check-in = board 1 start
  -- Day offsets (from v_anchor) intentionally left as missed check-ins.
  v_miss_offsets int[] := array[8, 12, 18, 23, 34, 41];
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
  raise notice 'Seeding history boards for user %', v_owner;

  -- ---------------------------------------------------------------------------
  -- Optional: deterministic reset of this user's check-in history only
  -- ---------------------------------------------------------------------------
  if v_reset_history then
    delete from public.check_ins ci using public.contacts c
      where ci.contact_id = c.id and c.owner_id = v_owner;
    delete from public.missed_check_ins m using public.contacts c
      where m.contact_id = c.id and c.owner_id = v_owner;
    raise notice 'Reset check-in history for user %.', v_owner;
  end if;

  -- ---------------------------------------------------------------------------
  -- Pick up to 3 existing contacts, or create 3 test contacts if none exist
  -- ---------------------------------------------------------------------------
  select array_agg(id order by created_at) into v_contact_ids
    from (select id, created_at
            from public.contacts
           where owner_id = v_owner
           order by created_at
           limit 3) c;

  if v_contact_ids is null or cardinality(v_contact_ids) = 0 then
    insert into public.contacts
      (owner_id, name, avatar_color, check_in_frequency, reminder_time)
    values
      (v_owner, 'Maya Patel', '#007AFF', 'daily', '12:00:00'),
      (v_owner, 'Leo Alvarez', '#FFCC33', 'daily', '12:00:00'),
      (v_owner, 'Sam Okafor', '#34C759', 'daily', '12:00:00');
    select array_agg(id) into v_contact_ids
      from public.contacts where owner_id = v_owner;
    raise notice 'Created 3 test contacts.';
  end if;
  raise notice 'Seeding boards across % contact(s).', cardinality(v_contact_ids);

  -- ---------------------------------------------------------------------------
  -- Insert daily check-ins: every day from anchor..anchor+84, EXCEPT the
  -- explicitly-missed offsets (those become gap cells via missed_check_ins).
  -- ---------------------------------------------------------------------------
  foreach v_c in array v_contact_ids loop
    insert into public.check_ins (contact_id, checked_in_at, note)
    select v_c,
           (v_anchor + g.days)::timestamptz + interval '12 hours',
           case
             when g.days between 0 and 25  then 'Board 1 check-in'
             when g.days between 26 and 51 then 'Board 2 check-in'
             when g.days between 52 and 77 then 'Board 3 check-in'
             else 'Current board check-in'
           end
      from generate_series(0, 84) as g(days)
     where not (g.days = any (v_miss_offsets));

    -- Missing rows -> the missed gap cells in boards 1 and 2.
    insert into public.missed_check_ins (contact_id, scheduled_date)
    select v_c, v_anchor + m.off
      from (select unnest(v_miss_offsets) as off) m
    on conflict (contact_id, scheduled_date) do nothing;

    -- Keep the denormalized schedule coherent with the seeded history.
    update public.contacts
       set last_check_in_date = current_date,
           next_check_in_date = current_date + 1,
           streak_count       = 3,   -- 3 boards completed
           updated_at         = now()
     where id = v_c;
  end loop;

  raise notice 'Done. Boards 1-3 finished (Board 3 Perfect), current board at 7/26 dots. Open History from the Home status card.';
end $$;