-- SECURITY HARDENING: Restrict raw profile access and enforce bans at DB level.
--
-- Part 1: Restrict direct SELECT on profiles table.
--   - Users can only read their own row from the raw table.
--   - Other users' profiles are accessible only via profiles_public_view (email masked).
--
-- Part 2: Add server-side ban enforcement inside the post/comment RPCs.
--   - create_community_post: check banned_users before inserting.
--   - create_community_comment: check banned_users before inserting.
--
-- PREREQUISITE: Firebase third-party JWT must be configured in Supabase.
--   run AFTER 20260410003_lockdown_rls_policies.sql

-- ── Part 1: Restrict profiles SELECT ────────────────────────────────────────

-- Drop existing policies (both the open one and any previous version of own-only)
drop policy if exists profiles_select_all on public.profiles;
drop policy if exists profiles_select_own on public.profiles;

-- New policy: a user can only see their own row directly from the table.
-- All "other user" reads must go through profiles_public_view.
create policy profiles_select_own
on public.profiles
for select
to public
using (firebase_uid = public.request_uid());

-- CRITICAL: Recreate profiles_public_view with security_invoker=off so it runs
-- as the view owner (superuser), bypassing the own-only RLS on the profiles table.
-- The CASE statement inside the view still correctly masks email for non-owners.
drop view if exists public.profiles_public_view;
create view public.profiles_public_view
with (security_invoker = off)
as
select
    firebase_uid,
    display_name,
    username_lower,
    case
        when firebase_uid = public.request_uid() then email
        else null
    end as email,
    photo_url,
    follower_count,
    following_count,
    post_count,
    rules_accepted,
    is_admin,
    is_mod,
    collection_public,
    wishlist_public,
    selected_avatar_id,
    custom_avatar_url,
    google_username_onboarding_required,
    google_username_onboarding_completed,
    privacy_accepted,
    created_at
from public.profiles;

grant select on public.profiles_public_view to anon, authenticated;


-- ── Part 2: Update create_community_post RPC with ban check ─────────────────

-- Also drop old overloads that may exist from previous migrations
drop function if exists public.create_community_post(text, text, boolean, boolean, integer, text, text, text, integer, text, text, text, text);
drop function if exists public.create_community_post(text, text, boolean, integer, text, text, text, integer, text, text, text, text);
create or replace function public.create_community_post(
    p_author_uid text,
    p_author_username text,
    p_author_is_admin boolean default false,
    p_author_is_mod boolean default false,
    p_author_selected_avatar_id integer default 1,
    p_author_custom_avatar_url text default null,
    p_car_model_name text default null,
    p_car_brand text default null,
    p_car_year integer default null,
    p_car_series text default null,
    p_car_image_url text default null,
    p_caption text default null,
    p_car_feature text default null
)
returns table (id uuid)
language plpgsql
security definer
set search_path = public
as $$
declare
    v_recent_post_count integer;
    v_caller_uid text;
begin
    -- Identity check: caller must match p_author_uid
    v_caller_uid := public.request_uid();
    if v_caller_uid is null or v_caller_uid != p_author_uid then
        raise exception 'Identity mismatch: caller UID does not match p_author_uid.';
    end if;

    -- Re-fetch is_admin and is_mod from DB (ignore client-supplied values)
    select is_admin, is_mod into p_author_is_admin, p_author_is_mod
    from public.profiles where firebase_uid = v_caller_uid;

    -- Ban check: reject if user is in banned_users
    if exists (
        select 1 from public.banned_users where user_uid = v_caller_uid
    ) then
        raise exception 'Forumdan engellendiniz. Gönderi oluşturamazsınız.';
    end if;

    -- Rate limit: max 2 posts per 5 minutes
    select count(*)
    into v_recent_post_count
    from public.community_posts
    where author_uid = p_author_uid
      and created_at > now() - interval '5 minutes';

    if v_recent_post_count >= 2 then
        raise exception 'Rate limit exceeded: Maximum 2 posts per 5 minutes.';
    end if;

    perform set_config('app.social_write_via_rpc', '1', true);

    return query
    insert into public.community_posts (
        author_uid,
        author_username,
        author_is_admin,
        author_is_mod,
        author_selected_avatar_id,
        author_custom_avatar_url,
        car_model_name,
        car_brand,
        car_year,
        car_series,
        car_image_url,
        caption,
        car_feature,
        is_active
    )
    values (
        p_author_uid,
        p_author_username,
        coalesce(p_author_is_admin, false),
        coalesce(p_author_is_mod, false),
        coalesce(p_author_selected_avatar_id, 1),
        p_author_custom_avatar_url,
        p_car_model_name,
        p_car_brand,
        p_car_year,
        p_car_series,
        p_car_image_url,
        p_caption,
        p_car_feature,
        true
    )
    returning community_posts.id;
end;
$$;

revoke all on function public.create_community_post(text, text, boolean, boolean, integer, text, text, text, integer, text, text, text, text) from public;
grant execute on function public.create_community_post(text, text, boolean, boolean, integer, text, text, text, integer, text, text, text, text) to anon, authenticated;

-- ── Part 3: Update create_community_comment RPC with ban check ──────────────

drop function if exists public.create_community_comment(uuid, text, text, boolean, boolean, text);
drop function if exists public.create_community_comment(uuid, text, text, boolean, text);
create or replace function public.create_community_comment(
    p_post_id uuid,
    p_author_uid text,
    p_author_username text,
    p_author_is_admin boolean default false,
    p_author_is_mod boolean default false,
    p_text text default null
)
returns setof public.community_comments
language plpgsql
security definer
set search_path = public
as $$
declare
    v_recent_comment_count integer;
    v_caller_uid text;
begin
    -- Identity check: caller must match p_author_uid
    v_caller_uid := public.request_uid();
    if v_caller_uid is null or v_caller_uid != p_author_uid then
        raise exception 'Identity mismatch: caller UID does not match p_author_uid.';
    end if;

    -- Re-fetch is_admin and is_mod from DB (ignore client-supplied values)
    select is_admin, is_mod into p_author_is_admin, p_author_is_mod
    from public.profiles where firebase_uid = v_caller_uid;

    -- Ban check: reject if user is in banned_users
    if exists (
        select 1 from public.banned_users where user_uid = v_caller_uid
    ) then
        raise exception 'Forumdan engellendiniz. Yorum yazamazsınız.';
    end if;

    -- Rate limit: max 5 comments per 1 minute
    select count(*)
    into v_recent_comment_count
    from public.community_comments
    where author_uid = p_author_uid
      and created_at > now() - interval '1 minute';

    if v_recent_comment_count >= 5 then
        raise exception 'Rate limit exceeded: Maximum 5 comments per 1 minute.';
    end if;

    perform set_config('app.social_write_via_rpc', '1', true);

    return query
    insert into public.community_comments (
        post_id,
        author_uid,
        author_username,
        author_is_admin,
        author_is_mod,
        text
    )
    values (
        p_post_id,
        p_author_uid,
        p_author_username,
        coalesce(p_author_is_admin, false),
        coalesce(p_author_is_mod, false),
        p_text
    )
    returning *;
end;
$$;

revoke all on function public.create_community_comment(uuid, text, text, boolean, boolean, text) from public;
grant execute on function public.create_community_comment(uuid, text, text, boolean, boolean, text) to anon, authenticated;

-- HARDENING COMPLETE

-- Reload PostgREST schema cache so new function signatures take effect immediately.
NOTIFY pgrst, 'reload schema';

