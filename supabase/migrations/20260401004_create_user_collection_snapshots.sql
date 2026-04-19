create table if not exists public.user_collection_snapshots (
    firebase_uid text primary key,
    payload_text text not null,
    updated_at timestamptz not null default now()
);

create index if not exists idx_user_collection_snapshots_updated_at
    on public.user_collection_snapshots(updated_at desc);

alter table public.user_collection_snapshots enable row level security;

drop policy if exists collection_snapshots_select_owner on public.user_collection_snapshots;
create policy collection_snapshots_select_owner
on public.user_collection_snapshots
for select
to public
using (firebase_uid = public.request_uid());

drop policy if exists collection_snapshots_insert_owner on public.user_collection_snapshots;
create policy collection_snapshots_insert_owner
on public.user_collection_snapshots
for insert
to public
with check (firebase_uid = public.request_uid());

drop policy if exists collection_snapshots_update_owner on public.user_collection_snapshots;
create policy collection_snapshots_update_owner
on public.user_collection_snapshots
for update
to public
using (firebase_uid = public.request_uid())
with check (firebase_uid = public.request_uid());

drop policy if exists collection_snapshots_delete_owner on public.user_collection_snapshots;
create policy collection_snapshots_delete_owner
on public.user_collection_snapshots
for delete
to public
using (firebase_uid = public.request_uid());

create or replace function public.touch_collection_snapshot_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists tg_touch_collection_snapshot_updated_at on public.user_collection_snapshots;
create trigger tg_touch_collection_snapshot_updated_at
before update on public.user_collection_snapshots
for each row
execute function public.touch_collection_snapshot_updated_at();
