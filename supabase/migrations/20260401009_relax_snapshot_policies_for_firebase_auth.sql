-- Temporary compatibility policy for FirebaseAuth client without Supabase JWT claim mapping.

alter table public.user_collection_snapshots enable row level security;

drop policy if exists collection_snapshots_select_owner on public.user_collection_snapshots;
drop policy if exists collection_snapshots_insert_owner on public.user_collection_snapshots;
drop policy if exists collection_snapshots_update_owner on public.user_collection_snapshots;
drop policy if exists collection_snapshots_delete_owner on public.user_collection_snapshots;

create policy collection_snapshots_select_all
on public.user_collection_snapshots
for select
to public
using (true);

create policy collection_snapshots_insert_all
on public.user_collection_snapshots
for insert
to public
with check (true);

create policy collection_snapshots_update_all
on public.user_collection_snapshots
for update
to public
using (true)
with check (true);

create policy collection_snapshots_delete_all
on public.user_collection_snapshots
for delete
to public
using (true);
