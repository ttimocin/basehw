-- Diecast news feed (read-only for app clients; writes via service role / dashboard)

create extension if not exists pgcrypto;

create table if not exists public.diecast_news (
    id uuid primary key default gen_random_uuid(),
    title text not null,
    body text not null,
    image_url text not null,
    published_at timestamptz not null default now(),
    is_published boolean not null default true
);

create index if not exists idx_diecast_news_published_at
    on public.diecast_news (published_at desc);

alter table public.diecast_news enable row level security;

drop policy if exists diecast_news_select_published on public.diecast_news;
create policy diecast_news_select_published
on public.diecast_news
for select
to public
using (is_published = true);
