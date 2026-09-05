-- =============================================================================
-- Foster — avatars storage bucket hardening
-- -----------------------------------------------------------------------------
-- The avatars bucket is public-read by design (profile photos render from a
-- public URL), so the fixes are: owner-scoped INSERT/DELETE policies (no free
-- file hosting for strangers) and bucket-level MIME + size limits (Supabase
-- keeps those on the bucket itself, NOT inside policy clauses).
-- Run in the Supabase SQL editor. Idempotent.
-- =============================================================================

-- 1. Bucket-level settings: only real images, max 2 MB --------------------------

update storage.buckets
   set allowed_mime_types = array['image/jpeg', 'image/png', 'image/webp'],
       file_size_limit    = 2097152
 where id = 'avatars';

-- 2. Public read stays (avatars are public by design) ---------------------------

drop policy if exists "avatars_public_read" on storage.objects;
create policy "avatars_public_read"
  on storage.objects for select
  using (bucket_id = 'avatars');

-- 3. Owner-scoped writes: users may only write INSIDE avatars/<their-uid>/… ----
-- storage.foldername(name) returns text[], so scope on [1] = the uid segment.

drop policy if exists "avatars_owner_insert" on storage.objects;
create policy "avatars_owner_insert"
  on storage.objects for insert to authenticated
  with check (
    bucket_id = 'avatars'
    and storage.foldername(name)[1] = auth.uid()::text
  );

drop policy if exists "avatars_owner_delete" on storage.objects;
create policy "avatars_owner_delete"
  on storage.objects for delete to authenticated
  using (
    bucket_id = 'avatars'
    and storage.foldername(name)[1] = auth.uid()::text
  );