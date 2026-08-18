-- =============================================================================
-- Badges feature migration: unlockable plant rewards based on total check-in
-- count (across ALL the user's contacts).
--
--   * adds `badges.description` (missing in migration_v2_erd.sql)
--   * seeds the 8-badge public catalog (thresholds 1 / 15 / 30 / 45 / 60 / 75 / 90 / 115)
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
-- Rename the existing catalog rows in place so existing user_badges rows keep
-- pointing at the same records while the catalog adopts the new sequence.
-- Use temporary names first. Existing badge names are unique, so prefixing the
-- original name keeps every temporary value unique. PostgreSQL checks the
-- unique name constraint during an UPDATE, so directly changing Yellow Flower
-- to Blue Flower would collide with the existing Blue Flower row before that
-- row can be renamed to Pink Flower.
update public.badges
set name = '__nekko_badge_migration__' || name
where name in (
  'Seedling', 'Soil', 'Green Flower', 'Wild Flower', 'Lotus Flower',
  'Mushroom Flower', 'Sunflower', 'Grove Keeper', 'Red Flower',
  'Yellow Flower', 'Blue Flower', 'Brown Flower', 'Pink Flower',
  'Towering Oak', 'Mushrooms'
);

update public.badges
set name = case
      when name = '__nekko_badge_migration__Seedling'
        or name = '__nekko_badge_migration__Soil'
        or (name = '__nekko_badge_migration__Green Flower' and threshold = 1)
        then 'Soil'
      when name = '__nekko_badge_migration__Wild Flower'
        or name = '__nekko_badge_migration__Lotus Flower'
        then 'Lotus Flower'
      when name = '__nekko_badge_migration__Mushroom Flower'
        or (name = '__nekko_badge_migration__Sunflower' and threshold = 30)
        then 'Sunflower'
      when name = '__nekko_badge_migration__Grove Keeper'
        or name = '__nekko_badge_migration__Red Flower'
        or name = '__nekko_badge_migration__Brown Flower'
        then 'Brown Flower'
      when name = '__nekko_badge_migration__Yellow Flower'
        or (name = '__nekko_badge_migration__Blue Flower' and threshold = 60)
        then 'Blue Flower'
      when name = '__nekko_badge_migration__Blue Flower' and threshold = 100
        or name = '__nekko_badge_migration__Pink Flower'
        then 'Pink Flower'
      when name = '__nekko_badge_migration__Towering Oak'
        or name = '__nekko_badge_migration__Green Flower'
        or (name = '__nekko_badge_migration__Sunflower' and threshold = 150)
        then 'Green Flower'
      when name = '__nekko_badge_migration__Mushrooms'
        then 'Mushrooms'
      else name
    end,
    description = case
      when name = '__nekko_badge_migration__Seedling'
        or name = '__nekko_badge_migration__Soil'
        or (name = '__nekko_badge_migration__Green Flower' and threshold = 1)
        then 'Your very first check-in starts the soil.'
      when name = '__nekko_badge_migration__Wild Flower'
        or name = '__nekko_badge_migration__Lotus Flower'
        then 'Reach 15 check-ins and your lotus flower begins to bloom.'
      when name = '__nekko_badge_migration__Mushroom Flower'
        or (name = '__nekko_badge_migration__Sunflower' and threshold = 30)
        then 'Reach 30 check-ins and grow a sunflower.'
      when name = '__nekko_badge_migration__Grove Keeper'
        or name = '__nekko_badge_migration__Red Flower'
        or name = '__nekko_badge_migration__Brown Flower'
        then 'Reach 45 check-ins and grow a brown flower.'
      when name = '__nekko_badge_migration__Yellow Flower'
        or (name = '__nekko_badge_migration__Blue Flower' and threshold = 60)
        then 'Reach 60 check-ins and grow a blue lotus.'
      when name = '__nekko_badge_migration__Blue Flower' and threshold = 100
        or name = '__nekko_badge_migration__Pink Flower'
        then 'Reach 75 check-ins and grow a pink flower.'
      when name = '__nekko_badge_migration__Towering Oak'
        or name = '__nekko_badge_migration__Green Flower'
        or (name = '__nekko_badge_migration__Sunflower' and threshold = 150)
        then 'Reach 90 check-ins and grow a green flower.'
      when name = '__nekko_badge_migration__Mushrooms'
        then 'Reach 115 check-ins and discover the mushrooms.'
      else description
    end,
    threshold = case
      when name = '__nekko_badge_migration__Seedling'
        or name = '__nekko_badge_migration__Soil'
        or (name = '__nekko_badge_migration__Green Flower' and threshold = 1)
        then 1
      when name = '__nekko_badge_migration__Wild Flower'
        or name = '__nekko_badge_migration__Lotus Flower'
        then 15
      when name = '__nekko_badge_migration__Mushroom Flower'
        or (name = '__nekko_badge_migration__Sunflower' and threshold = 30)
        then 30
      when name = '__nekko_badge_migration__Grove Keeper'
        or name = '__nekko_badge_migration__Red Flower'
        or name = '__nekko_badge_migration__Brown Flower'
        then 45
      when name = '__nekko_badge_migration__Yellow Flower'
        or (name = '__nekko_badge_migration__Blue Flower' and threshold = 60)
        then 60
      when name = '__nekko_badge_migration__Blue Flower' and threshold = 100
        or name = '__nekko_badge_migration__Pink Flower'
        then 75
      when name = '__nekko_badge_migration__Towering Oak'
        or name = '__nekko_badge_migration__Green Flower'
        or (name = '__nekko_badge_migration__Sunflower' and threshold = 150)
        then 90
      when name = '__nekko_badge_migration__Mushrooms'
        then 115
      else threshold
    end
where name like '__nekko_badge_migration__%';

insert into public.badges (name, description, threshold)
select 'Soil', 'Your very first check-in starts the soil.', 1
where not exists (select 1 from public.badges where name = 'Soil');

insert into public.badges (name, description, threshold)
select 'Lotus Flower', 'Reach 15 check-ins and your lotus flower begins to bloom.', 15
where not exists (select 1 from public.badges where name = 'Lotus Flower');

insert into public.badges (name, description, threshold)
select 'Sunflower', 'Reach 30 check-ins and grow a sunflower.', 30
where not exists (select 1 from public.badges where name = 'Sunflower');

insert into public.badges (name, description, threshold)
select 'Brown Flower', 'Reach 45 check-ins and grow a brown flower.', 45
where not exists (select 1 from public.badges where name = 'Brown Flower');

insert into public.badges (name, description, threshold)
select 'Blue Flower', 'Reach 60 check-ins and grow a blue lotus.', 60
where not exists (select 1 from public.badges where name = 'Blue Flower');

insert into public.badges (name, description, threshold)
select 'Pink Flower', 'Reach 75 check-ins and grow a pink flower.', 75
where not exists (select 1 from public.badges where name = 'Pink Flower');

insert into public.badges (name, description, threshold)
select 'Green Flower', 'Reach 90 check-ins and grow a green flower.', 90
where not exists (select 1 from public.badges where name = 'Green Flower');

insert into public.badges (name, description, threshold)
select 'Mushrooms', 'Reach 115 check-ins and discover the mushrooms.', 115
where not exists (select 1 from public.badges where name = 'Mushrooms');

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
