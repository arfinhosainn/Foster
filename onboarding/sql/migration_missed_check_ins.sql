-- Durable missed-occurrence history for the Home check-in calendar.
-- Apply after migration_v2_erd.sql to update an existing Supabase database.

create table if not exists public.missed_check_ins (
  id             uuid not null default gen_random_uuid(),
  contact_id     uuid not null,
  scheduled_date date not null,
  created_at     timestamptz not null default now(),
  constraint missed_check_ins_pkey primary key (id),
  constraint missed_check_ins_contact_id_fkey foreign key (contact_id)
    references public.contacts(id) on delete cascade,
  constraint missed_check_ins_contact_date_key unique (contact_id, scheduled_date)
);

create index if not exists idx_missed_check_ins_date
  on public.missed_check_ins(scheduled_date, contact_id);

grant select, insert on public.missed_check_ins to authenticated;

alter table public.missed_check_ins enable row level security;
drop policy if exists "missed_check_ins_select_own" on public.missed_check_ins;
create policy "missed_check_ins_select_own" on public.missed_check_ins for select
  using (
    exists (select 1 from public.contacts c where c.id = contact_id and c.owner_id = auth.uid())
  );
drop policy if exists "missed_check_ins_insert_own" on public.missed_check_ins;
create policy "missed_check_ins_insert_own" on public.missed_check_ins for insert
  with check (
    exists (select 1 from public.contacts c where c.id = contact_id and c.owner_id = auth.uid())
  );

create or replace function public.next_scheduled_check_in_date(
  p_date date,
  p_frequency text
)
returns date
language plpgsql
immutable
set search_path = public
as $$
declare
  target_month date;
  last_day date;
  target_day integer;
begin
  case p_frequency
    when 'daily' then return p_date + 1;
    when 'weekly' then return p_date + 7;
    when 'biweekly' then return p_date + 14;
    when 'monthly' then
      target_month := (date_trunc('month', p_date::timestamp) + interval '1 month')::date;
      last_day := (date_trunc('month', target_month::timestamp) + interval '1 month' - interval '1 day')::date;
      target_day := least(extract(day from p_date)::integer, extract(day from last_day)::integer);
      return target_month + target_day - 1;
    else return null;
  end case;
end;
$$;

create or replace function public.sync_missed_check_ins(p_as_of_date date)
returns void
language plpgsql
security invoker
set search_path = public
as $$
declare
  contact_row record;
  due_date date;
  next_date date;
begin
  for contact_row in
    select id, next_check_in_date, check_in_frequency
      from public.contacts
     where owner_id = auth.uid()
       and next_check_in_date is not null
  loop
    due_date := contact_row.next_check_in_date;
    while due_date < p_as_of_date loop
      insert into public.missed_check_ins (contact_id, scheduled_date)
      values (contact_row.id, due_date)
      on conflict (contact_id, scheduled_date) do nothing;

      next_date := public.next_scheduled_check_in_date(
        due_date,
        contact_row.check_in_frequency
      );
      exit when next_date is null or next_date <= due_date;
      due_date := next_date;
    end loop;
  end loop;
end;
$$;

grant execute on function public.sync_missed_check_ins(date) to authenticated;

create or replace function public.log_check_in(
  p_contact_id uuid,
  p_last_check_in_date date,
  p_next_check_in_date date,
  p_streak_count integer,
  p_checked_in_at timestamptz
)
returns public.contacts
language plpgsql
security invoker
set search_path = public
as $$
declare
  updated_contact public.contacts;
begin
  perform public.sync_missed_check_ins(p_last_check_in_date);

  update public.contacts
  set
    last_check_in_date = p_last_check_in_date,
    next_check_in_date = p_next_check_in_date,
    streak_count = p_streak_count
  where id = p_contact_id
    and owner_id = auth.uid()
    and (
      next_check_in_date <= p_last_check_in_date
      or (
        next_check_in_date is null
        and (
          last_check_in_date is null
          or last_check_in_date < p_last_check_in_date
        )
      )
    )
  returning * into updated_contact;

  if not found then
    raise exception 'CONTACT_NOT_DUE';
  end if;

  insert into public.check_ins (contact_id, checked_in_at)
  values (p_contact_id, p_checked_in_at);

  return updated_contact;
end;
$$;

grant execute on function public.log_check_in(uuid, date, date, integer, timestamptz)
  to authenticated;