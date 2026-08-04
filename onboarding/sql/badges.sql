-- =============================================================================
-- Badges feature migration: unlockable plant rewards based on total check-in
-- count (across ALL the user's contacts).
--
--   * adds `badges.description` (missing in migration_v2_erd.sql)
--   * seeds the 4-badge public catalog (thresholds 1 / 15 / 50 / 150)
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
insert into public.badges (name, description, threshold)
select 'Seedling', 'Your very first check-in. A plant is born.', 1
where not exists (select 1 from public.badges where name = 'Seedling');

insert into public.badges (name, description, threshold)
select 'Wild Flower', 'Reach 15 check-ins and your plant begins to bloom.', 15
where not exists (select 1 from public.badges where name = 'Wild Flower');

insert into public.badges (name, description, threshold)
select 'Grove Keeper', 'Reach 50 check-ins and your plant grows into a grove.', 50
where not exists (select 1 from public.badges where name = 'Grove Keeper');

insert into public.badges (name, description, threshold)
select 'Towering Oak', 'Reach 150 check-ins and grow a towering tree.', 150
where not exists (select 1 from public.badges where name = 'Towering Oak');

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
