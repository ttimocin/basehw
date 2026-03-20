-- Storage: Firebase JWT tabanlı kullanıcı-path policy'leri
--
-- Ön koşul (Supabase Dashboard → Authentication → Providers → Add third-party JWT):
--   JWKS URL : https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com
--   Issuer   : https://securetoken.google.com/YOUR_FIREBASE_PROJECT_ID
--
-- Bu SQL'i Supabase Dashboard → SQL Editor içinde çalıştır.

-- ── Eski anon policy'leri kaldır ─────────────────────────────────────────────
drop policy if exists photo_backup_select_anon  on storage.objects;
drop policy if exists photo_backup_insert_anon  on storage.objects;
drop policy if exists photo_backup_update_anon  on storage.objects;
drop policy if exists photo_backup_delete_anon  on storage.objects;
drop policy if exists photo_backup_select_own   on storage.objects;
drop policy if exists photo_backup_insert_own   on storage.objects;
drop policy if exists photo_backup_update_own   on storage.objects;
drop policy if exists photo_backup_delete_own   on storage.objects;

-- ── Firebase UID ile user-path kontrolü (authenticated rol) ──────────────────
--
-- Dosya yolu: {firebase_uid}/cars/{carId}.jpg
-- auth.jwt() ->> 'sub'  →  Firebase'deki uid değeri
-- (storage.foldername(name))[1] → yolun ilk klasörü (uid klasörü)
--
-- Not: Third-party JWT istekleri storage tarafında her zaman `authenticated`
-- DB rolüne düşmeyebilir. Bu yüzden rol kısıtı yerine claim/path eşleşmesine
-- güveniyoruz. JWT yoksa `sub` null kalır ve policy yine geçmez.

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

-- Doğrulama:
-- select policyname, cmd, roles
-- from pg_policies
-- where schemaname = 'storage'
--   and tablename = 'objects'
--   and policyname like 'photo_backup_%'
-- order by policyname;
