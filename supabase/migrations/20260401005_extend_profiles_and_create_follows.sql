-- Extend social profile data and move follows graph to Supabase.

alter table if exists public.profiles
    add column if not exists email text,
    add column if not exists is_admin boolean not null default false,
    add column if not exists follower_count integer not null default 0,
    add column if not exists following_count integer not null default 0,
    add column if not exists post_count integer not null default 0,
    add column if not exists rules_accepted boolean not null default false,
    add column if not exists collection_public boolean not null default false,
    add column if not exists wishlist_public boolean not null default false;

create table if not exists public.follows (
    follower_uid text not null,
    followed_uid text not null,
    created_at timestamptz not null default now(),
    primary key (follower_uid, followed_uid)
);

create index if not exists idx_follows_followed_uid
    on public.follows (followed_uid);

alter table public.follows enable row level security;

drop policy if exists profiles_select_own on public.profiles;
drop policy if exists profiles_select_all on public.profiles;
create policy profiles_select_all
on public.profiles
for select
to public
using (true);

drop policy if exists profiles_insert_own on public.profiles;
create policy profiles_insert_own
on public.profiles
for insert
to public
with check (firebase_uid = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, ''));

drop policy if exists profiles_update_own on public.profiles;
create policy profiles_update_own
on public.profiles
for update
to public
using (firebase_uid = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, ''))
with check (firebase_uid = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, ''));

drop policy if exists follows_select_participant on public.follows;
create policy follows_select_participant
on public.follows
for select
to public
using (
    follower_uid = public.request_uid()
    or followed_uid = public.request_uid()
);

drop policy if exists follows_insert_owner on public.follows;
create policy follows_insert_owner
on public.follows
for insert
to public
with check (follower_uid = public.request_uid());

drop policy if exists follows_delete_owner on public.follows;
create policy follows_delete_owner
on public.follows
for delete
to public
using (follower_uid = public.request_uid());
