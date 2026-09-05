-- =============================================================================
-- Foster — drop contacts.phone_number
-- -----------------------------------------------------------------------------
-- Foster no longer collects phone numbers (data minimization: SMS hand-off was
-- replaced by the system share sheet, and no "Send to {name}" deep link exists
-- anymore). Dropping the column also purges any numbers stored by older app
-- versions. Run in the live Supabase SQL editor.
-- =============================================================================
alter table public.contacts drop column if exists phone_number;