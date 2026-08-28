-- Adds the contact's phone number (captured when imported from the phone's
-- contacts) so brainstorm topics can hand off to the SMS app with the
-- recipient pre-filled. Nullable: manually-created contacts have no number.
alter table public.contacts add column if not exists phone_number text;
