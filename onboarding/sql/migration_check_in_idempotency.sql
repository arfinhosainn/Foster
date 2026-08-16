-- Idempotent check-in write.
-- Apply after migration_v2_erd.sql. The function advances the contact and inserts
-- the event in one transaction, and refuses a contact whose next check-in is not due.

create or replace function public.log_check_in(
  p_contact_id uuid,
  p_last_check_in_date date,
  p_next_check_in_date date,
  p_streak_count integer
)
returns public.contacts
language plpgsql
security invoker
set search_path = public
as $$
declare
  updated_contact public.contacts;
begin
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

  insert into public.check_ins (contact_id)
  values (p_contact_id);

  return updated_contact;
end;
$$;

grant execute on function public.log_check_in(uuid, date, date, integer)
  to authenticated;