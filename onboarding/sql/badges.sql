-- =============================================================================
-- Badges feature migration: unlockable plant rewards based on total check-in
-- count (across ALL the user's contacts).
--
--   * adds `badges.description` (missing in migration_v2_erd.sql)
--   * seeds the 7-badge public catalog (thresholds 1 / 15 / 30 / 50 / 75 / 100 / 150)
--   * auto-unlocks badges via a `check_ins` AFTER INSERT trigger
--
-- Idempotent / re-runnable: column adds are guarded, seeds only insert rows
-- that are absent, and the trigger is created with `drop ... if exists`.
-- =============================================================================

-- 1. Description column (reveal screen shows it; absent from v2 ERD).
do $$
begin
  if not exists (
    select 1 from information_schema.columns
    where table_schema = 'public'
      and table_name = 'badges'
      and column_name = 'description'
  ) then
    alter table public.badges add column description text;
  end if;
end $$;

-- 2. Seed catalog. Each insert is guarded so re-runs never duplicate and never
--    clobber admin edits to the name/description of an existing badge.
--
-- Rename the original four catalog rows in place so existing user_badges rows
-- keep pointing at the same records while the catalog adopts the seven flower
-- names. These updates are intentionally limited to the original seed names.
update public.badges
set name = 'Green Flower',
    description = 'Your very first check-in. A green flower begins to grow.'
where name = 'Seedling';

update public.badges
set name = 'Lotus Flower',
    description = 'Reach 15 check-ins and your lotus flower begins to bloom.'
where name = 'Wild Flower';

update public.badges
set name = 'Red Flower',
    description = 'Reach 50 check-ins and your red flower begins to bloom.'
where name = 'Grove Keeper';

update public.badges
set name = 'Sunflower',
    description = 'Reach 150 check-ins and grow a sunflower.'
where name = 'Towering Oak';

insert into public.badges (name, description, threshold)
select 'Green Flower', 'Your very first check-in. A green flower begins to grow.', 1
where not exists (select 1 from public.badges where name = 'Green Flower');

insert into public.badges (name, description, threshold)
select 'Lotus Flower', 'Reach 15 check-ins and your lotus flower begins to bloom.', 15
where not exists (select 1 from public.badges where name = 'Lotus Flower');

insert into public.badges (name, description, threshold)
select 'Mushroom Flower', 'Reach 30 check-ins and discover a mushroom flower.', 30
where not exists (select 1 from public.badges where name = 'Mushroom Flower');

insert into public.badges (name, description, threshold)
select 'Red Flower', 'Reach 50 check-ins and your red flower begins to bloom.', 50
where not exists (select 1 from public.badges where name = 'Red Flower');

insert into public.badges (name, description, threshold)
select 'Yellow Flower', 'Reach 75 check-ins and grow a bright yellow flower.', 75
where not exists (select 1 from public.badges where name = 'Yellow Flower');

insert into public.badges (name, description, threshold)
select 'Blue Flower', 'Reach 100 check-ins and grow a calm blue flower.', 100
where not exists (select 1 from public.badges where name = 'Blue Flower');

insert into public.badges (name, description, threshold)
select 'Sunflower', 'Reach 150 check-ins and grow a sunflower.', 150
where not exists (select 1 from public.badges where name = 'Sunflower');

-- 3. Auto-unlock trigger. On every new check-in, unlock any badge whose
--    threshold the user's all-time check-in count has reached. The count is
--    across ALL the user's contacts via a two-hop join (check_ins ->
--    contacts -> owner). RLS is bypassed inside (security definer) and we read
--    the owner from `new.contact_id` — never from caller-supplied input.
create or replace function public.unlock_badges_on_check_in()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_total integer;
  v_owner uuid;
  b record;
begin
  select owner_id into v_owner
  from public.contacts
  where id = new.contact_id;

  if v_owner is null then
    return new;
  end if;

  select count(*)
  into v_total
  from public.check_ins ci
  join public.contacts c on c.id = ci.contact_id
  where c.owner_id = v_owner;

  for b in
    select id, threshold
    from public.badges
    where threshold is not null
    order by threshold asc
  loop
    if v_total >= b.threshold then
      insert into public.user_badges (owner_id, badge_id)
      values (v_owner, b.id)
      on conflict (owner_id, badge_id) do nothing;
    end if;
  end loop;

  return new;
end;
$$;

drop trigger if exists trg_unlock_badges_on_check_in on public.check_ins;
create trigger trg_unlock_badges_on_check_in
  after insert on public.check_ins
  for each row execute function public.unlock_badges_on_check_in();
