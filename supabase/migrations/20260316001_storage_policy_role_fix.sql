-- Storage policy fix for third-party JWT uploads.
--
-- Some storage requests with third-party JWTs may not run under the
-- `authenticated` Postgres role even though `auth.jwt()` contains the claims.
-- We therefore rely on the JWT/path match instead of role membership.

drop policy if exists photo_backup_select_own on storage.objects;
drop policy if exists photo_backup_insert_own on storage.objects;
drop policy if exists photo_backup_update_own on storage.objects;
drop policy if exists photo_backup_delete_own on storage.objects;

create policy photo_backup_select_own
on storage.objects
for select
to public
using (
    bucket_id = 'user-car-photos'
    and (storage.foldername(name))[1] = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, '')
);

create policy photo_backup_insert_own
on storage.objects
for insert
to public
with check (
    bucket_id = 'user-car-photos'
    and (storage.foldername(name))[1] = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, '')
);

create policy photo_backup_update_own
on storage.objects
for update
to public
using (
    bucket_id = 'user-car-photos'
    and (storage.foldername(name))[1] = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, '')
)
with check (
    bucket_id = 'user-car-photos'
    and (storage.foldername(name))[1] = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, '')
);

create policy photo_backup_delete_own
on storage.objects
for delete
to public
using (
    bucket_id = 'user-car-photos'
    and (storage.foldername(name))[1] = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, '')
);