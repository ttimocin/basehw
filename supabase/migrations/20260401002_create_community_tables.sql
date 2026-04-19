-- Community feed tables (Supabase backend for posts/likes/comments)

create extension if not exists pgcrypto;

create table if not exists public.community_posts (
    id uuid primary key default gen_random_uuid(),
    author_uid text not null,
    author_username text not null,
    author_is_admin boolean not null default false,
    car_model_name text not null,
    car_brand text not null,
    car_year integer,
    car_series text,
    car_image_url text not null,
    caption text not null,
    car_feature text,
    like_count integer not null default 0,
    comment_count integer not null default 0,
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.community_likes (
    post_id uuid not null references public.community_posts(id) on delete cascade,
    user_uid text not null,
    created_at timestamptz not null default now(),
    primary key (post_id, user_uid)
);

create table if not exists public.community_comments (
    id uuid primary key default gen_random_uuid(),
    post_id uuid not null references public.community_posts(id) on delete cascade,
    author_uid text not null,
    author_username text not null,
    author_is_admin boolean not null default false,
    text text not null,
    created_at timestamptz not null default now()
);

create index if not exists idx_community_posts_created_at
    on public.community_posts (created_at desc);

create index if not exists idx_community_posts_author_uid
    on public.community_posts (author_uid);

create index if not exists idx_community_likes_user_uid
    on public.community_likes (user_uid);

create index if not exists idx_community_comments_post_id_created_at
    on public.community_comments (post_id, created_at asc);

alter table public.community_posts enable row level security;
alter table public.community_likes enable row level security;
alter table public.community_comments enable row level security;

-- NOTE: Initial migration keeps policies permissive to avoid breaking existing
-- Firebase-auth based app sessions that do not yet mint Supabase JWT user sessions.

drop policy if exists community_posts_select_all on public.community_posts;
drop policy if exists community_posts_insert_all on public.community_posts;
drop policy if exists community_posts_update_all on public.community_posts;
drop policy if exists community_posts_delete_all on public.community_posts;

create policy community_posts_select_all
on public.community_posts
for select
to public
using (true);

create policy community_posts_insert_all
on public.community_posts
for insert
to public
with check (true);

create policy community_posts_update_all
on public.community_posts
for update
to public
using (true)
with check (true);

create policy community_posts_delete_all
on public.community_posts
for delete
to public
using (true);

drop policy if exists community_likes_select_all on public.community_likes;
drop policy if exists community_likes_insert_all on public.community_likes;
drop policy if exists community_likes_delete_all on public.community_likes;

create policy community_likes_select_all
on public.community_likes
for select
to public
using (true);

create policy community_likes_insert_all
on public.community_likes
for insert
to public
with check (true);

create policy community_likes_delete_all
on public.community_likes
for delete
to public
using (true);

drop policy if exists community_comments_select_all on public.community_comments;
drop policy if exists community_comments_insert_all on public.community_comments;
drop policy if exists community_comments_delete_all on public.community_comments;

create policy community_comments_select_all
on public.community_comments
for select
to public
using (true);

create policy community_comments_insert_all
on public.community_comments
for insert
to public
with check (true);

create policy community_comments_delete_all
on public.community_comments
for delete
to public
using (true);
