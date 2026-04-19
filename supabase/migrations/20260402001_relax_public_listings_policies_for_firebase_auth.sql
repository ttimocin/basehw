-- App authenticates with FirebaseAuth, not Supabase JWT sessions, so request_uid() can be null.
-- Relax public_listings write policies to match other Firebase-compatible social tables.

alter table public.public_listings enable row level security;

drop policy if exists listings_insert_own on public.public_listings;
drop policy if exists listings_update_own on public.public_listings;
drop policy if exists listings_delete_own on public.public_listings;
drop policy if exists listings_insert_all on public.public_listings;
drop policy if exists listings_update_all on public.public_listings;
drop policy if exists listings_delete_all on public.public_listings;

-- keep select policy as-is (listings_select_visible => using true)

create policy listings_insert_all
on public.public_listings
for insert
to public
with check (true);

create policy listings_update_all
on public.public_listings
for update
to public
using (true)
with check (true);

create policy listings_delete_all
on public.public_listings
for delete
to public
using (true);
