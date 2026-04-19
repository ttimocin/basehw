-- Firebase UID mapping tables for Android app integration.
-- JWT claim mapping: auth.jwt() ->> 'sub' == firebase_uid

create extension if not exists pgcrypto;

create table if not exists public.profiles (
    firebase_uid text primary key,
    display_name text,
    photo_url text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.public_listings (
    listing_id text primary key,
    firebase_uid text not null references public.profiles(firebase_uid) on delete cascade,
    title text not null,
    image_url text,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.messages (
    id uuid primary key default gen_random_uuid(),
    conversation_id text not null,
    firebase_uid text not null references public.profiles(firebase_uid) on delete cascade,
    receiver_uid text not null,
    message_body text not null,
    created_at timestamptz not null default now()
);

alter table public.profiles enable row level security;
alter table public.public_listings enable row level security;
alter table public.messages enable row level security;

drop policy if exists profiles_select_own on public.profiles;
drop policy if exists profiles_insert_own on public.profiles;
drop policy if exists profiles_update_own on public.profiles;

create policy profiles_select_own
on public.profiles
for select
to public
using (firebase_uid = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, ''));

create policy profiles_insert_own
on public.profiles
for insert
to public
with check (firebase_uid = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, ''));

create policy profiles_update_own
on public.profiles
for update
to public
using (firebase_uid = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, ''))
with check (firebase_uid = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, ''));

drop policy if exists listings_select_visible on public.public_listings;
drop policy if exists listings_insert_own on public.public_listings;
drop policy if exists listings_update_own on public.public_listings;
drop policy if exists listings_delete_own on public.public_listings;

create policy listings_select_visible
on public.public_listings
for select
to public
using (true);

create policy listings_insert_own
on public.public_listings
for insert
to public
with check (firebase_uid = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, ''));

create policy listings_update_own
on public.public_listings
for update
to public
using (firebase_uid = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, ''))
with check (firebase_uid = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, ''));

create policy listings_delete_own
on public.public_listings
for delete
to public
using (firebase_uid = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, ''));

drop policy if exists messages_select_participant on public.messages;
drop policy if exists messages_insert_sender on public.messages;

create policy messages_select_participant
on public.messages
for select
to public
using (
    firebase_uid = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, '')
    or receiver_uid = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, '')
);

create policy messages_insert_sender
on public.messages
for insert
to public
with check (firebase_uid = coalesce(auth.jwt() ->> 'sub', auth.uid()::text, ''));
