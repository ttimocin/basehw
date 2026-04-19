-- LOCKDOWN: Restore proper RLS policies with auth.jwt() sub claim
-- This migration reverses the relax policies from 20260401008
-- and enforces proper identity-based access control.
--
-- PREREQUISITE: Firebase third-party JWT must be configured in Supabase:
--   Dashboard -> Authentication -> Providers -> Add third-party JWT
--   JWKS URL : https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com
--   Issuer   : https://securetoken.google.com/YOUR_FIREBASE_PROJECT_ID

-- Helper: Extract caller Firebase UID from JWT

create or replace function public.request_uid()
returns text
language sql
stable
as $$
    select nullif(
        coalesce(
            auth.jwt() ->> 'sub',
            current_setting('request.jwt.claim.sub', true)
        ),
        ''
    )
$$;

grant execute on function public.request_uid() to anon, authenticated;

-- 1. PROFILES

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

drop policy if exists profiles_update_all on public.profiles;
drop policy if exists profiles_insert_all on public.profiles;
drop policy if exists profiles_read_all on public.profiles;
drop policy if exists profiles_self_update on public.profiles;
drop policy if exists profiles_self_insert on public.profiles;
drop policy if exists profiles_select_all on public.profiles;
drop policy if exists profiles_insert_own on public.profiles;
drop policy if exists profiles_update_own on public.profiles;

create policy profiles_select_all
on public.profiles
for select
to public
using (true);

create policy profiles_insert_own
on public.profiles
for insert
to public
with check (firebase_uid = public.request_uid());

create policy profiles_update_own
on public.profiles
for update
to public
using (firebase_uid = public.request_uid())
with check (firebase_uid = public.request_uid());

-- 2. COMMUNITY POSTS

drop policy if exists community_posts_insert_all on public.community_posts;
drop policy if exists community_posts_update_all on public.community_posts;
drop policy if exists community_posts_delete_all on public.community_posts;
drop policy if exists community_posts_select_all on public.community_posts;
drop policy if exists community_posts_insert_owner on public.community_posts;
drop policy if exists community_posts_update_owner on public.community_posts;
drop policy if exists community_posts_delete_owner on public.community_posts;

create policy community_posts_select_all
on public.community_posts
for select
to public
using (true);

create policy community_posts_insert_owner
on public.community_posts
for insert
to public
with check (author_uid = public.request_uid());

create policy community_posts_update_owner
on public.community_posts
for update
to public
using (author_uid = public.request_uid())
with check (author_uid = public.request_uid());

create policy community_posts_delete_owner
on public.community_posts
for delete
to public
using (author_uid = public.request_uid());

-- 3. COMMUNITY COMMENTS

drop policy if exists community_comments_insert_all on public.community_comments;
drop policy if exists community_comments_delete_all on public.community_comments;
drop policy if exists community_comments_select_all on public.community_comments;
drop policy if exists community_comments_insert_owner on public.community_comments;
drop policy if exists community_comments_delete_owner_or_post_owner on public.community_comments;

create policy community_comments_select_all
on public.community_comments
for select
to public
using (true);

create policy community_comments_insert_owner
on public.community_comments
for insert
to public
with check (author_uid = public.request_uid());

create policy community_comments_delete_owner_or_post_owner
on public.community_comments
for delete
to public
using (
    author_uid = public.request_uid()
    or exists (
        select 1
        from public.community_posts p
        where p.id = community_comments.post_id
          and p.author_uid = public.request_uid()
    )
);

-- 4. COMMUNITY LIKES

drop policy if exists community_likes_insert_all on public.community_likes;
drop policy if exists community_likes_delete_all on public.community_likes;
drop policy if exists community_likes_select_all on public.community_likes;
drop policy if exists community_likes_insert_owner on public.community_likes;
drop policy if exists community_likes_delete_owner on public.community_likes;

create policy community_likes_select_all
on public.community_likes
for select
to public
using (true);

create policy community_likes_insert_owner
on public.community_likes
for insert
to public
with check (user_uid = public.request_uid());

create policy community_likes_delete_owner
on public.community_likes
for delete
to public
using (user_uid = public.request_uid());

-- 5. FOLLOWS

drop policy if exists follows_insert_all on public.follows;
drop policy if exists follows_delete_all on public.follows;
drop policy if exists follows_select_all on public.follows;
drop policy if exists follows_insert_owner on public.follows;
drop policy if exists follows_delete_owner on public.follows;

create policy follows_select_all
on public.follows
for select
to public
using (true);

create policy follows_insert_owner
on public.follows
for insert
to public
with check (follower_uid = public.request_uid());

create policy follows_delete_owner
on public.follows
for delete
to public
using (follower_uid = public.request_uid());

-- 6. BLOCKED_USERS / BANNED_USERS

drop policy if exists banned_users_read on public.banned_users;
drop policy if exists banned_users_manage on public.banned_users;
drop policy if exists banned_users_select_all on public.banned_users;
drop policy if exists banned_users_manage_admin on public.banned_users;

create policy banned_users_select_all
on public.banned_users
for select
to public
using (true);

create policy banned_users_manage_admin
on public.banned_users
for all
to public
using (
    exists (
        select 1 from public.profiles
        where firebase_uid = public.request_uid()
          and (is_admin = true or is_mod = true)
    )
);

-- 7. MESSAGES

drop policy if exists messages_select_participant on public.messages;
drop policy if exists messages_insert_sender on public.messages;
drop policy if exists messages_read_self on public.messages;

create policy messages_select_participant
on public.messages
for select
to public
using (
    firebase_uid = public.request_uid()
    or receiver_uid = public.request_uid()
);

create policy messages_insert_sender
on public.messages
for insert
to public
with check (firebase_uid = public.request_uid());

-- 8. PUBLIC_LISTINGS

drop policy if exists listings_read_all on public.public_listings;
drop policy if exists listings_self_all on public.public_listings;
drop policy if exists listings_select_all on public.public_listings;
drop policy if exists listings_insert_own on public.public_listings;
drop policy if exists listings_update_own on public.public_listings;
drop policy if exists listings_delete_own on public.public_listings;

create policy listings_select_all
on public.public_listings
for select
to public
using (true);

create policy listings_insert_own
on public.public_listings
for insert
to public
with check (firebase_uid = public.request_uid());

create policy listings_update_own
on public.public_listings
for update
to public
using (firebase_uid = public.request_uid())
with check (firebase_uid = public.request_uid());

create policy listings_delete_own
on public.public_listings
for delete
to public
using (firebase_uid = public.request_uid());

-- 9. FEEDBACK_MESSAGES

drop policy if exists feedback_messages_insert_all on public.feedback_messages;
drop policy if exists feedback_messages_insert_owner on public.feedback_messages;
drop policy if exists feedback_messages_select_own on public.feedback_messages;

create policy feedback_messages_select_own
on public.feedback_messages
for select
to public
using (firebase_uid = public.request_uid());

create policy feedback_messages_insert_own
on public.feedback_messages
for insert
to public
with check (firebase_uid = public.request_uid());

-- 10. USER_COLLECTION_SNAPSHOTS

drop policy if exists snapshots_self_all on public.user_collection_snapshots;
drop policy if exists snapshots_select_own on public.user_collection_snapshots;
drop policy if exists snapshots_insert_own on public.user_collection_snapshots;
drop policy if exists snapshots_update_own on public.user_collection_snapshots;
drop policy if exists snapshots_delete_own on public.user_collection_snapshots;

create policy snapshots_select_own
on public.user_collection_snapshots
for select
to public
using (firebase_uid = public.request_uid());

create policy snapshots_insert_own
on public.user_collection_snapshots
for insert
to public
with check (firebase_uid = public.request_uid());

create policy snapshots_update_own
on public.user_collection_snapshots
for update
to public
using (firebase_uid = public.request_uid())
with check (firebase_uid = public.request_uid());

create policy snapshots_delete_own
on public.user_collection_snapshots
for delete
to public
using (firebase_uid = public.request_uid());

-- 11. TRIGGER: Prevent self-admin promotion via profile UPDATE

drop function if exists public.prevent_self_privilege_escalation();
create or replace function public.prevent_self_privilege_escalation()
returns trigger
language plpgsql
as $$
begin
    if (NEW.is_admin IS DISTINCT FROM OLD.is_admin OR NEW.is_mod IS DISTINCT FROM OLD.is_mod) then
        if not exists (
            select 1 from public.profiles
            where firebase_uid = public.request_uid()
              and is_admin = true
        ) then
            raise exception 'Only admins can change is_admin or is_mod fields.';
        end if;
    end if;
    return NEW;
end;
$$;

drop trigger if exists trg_prevent_self_privilege_escalation on public.profiles;
create trigger trg_prevent_self_privilege_escalation
before update of is_admin, is_mod on public.profiles
for each row
execute function public.prevent_self_privilege_escalation();

-- 12. RPC: Admin-only delete any post

drop function if exists public.admin_delete_community_post(uuid);
create or replace function public.admin_delete_community_post(p_post_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    if not exists (
        select 1 from public.profiles
        where firebase_uid = public.request_uid()
          and (is_admin = true or is_mod = true)
    ) then
        raise exception 'Only admins or moderators can delete arbitrary posts.';
    end if;

    delete from public.community_posts where id = p_post_id;
end;
$$;

revoke all on function public.admin_delete_community_post(uuid) from public;
grant execute on function public.admin_delete_community_post(uuid) to anon, authenticated;

-- 13. RPC: Admin-only delete any comment

drop function if exists public.admin_delete_community_comment(uuid);
create or replace function public.admin_delete_community_comment(p_comment_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    if not exists (
        select 1 from public.profiles
        where firebase_uid = public.request_uid()
          and (is_admin = true or is_mod = true)
    ) then
        raise exception 'Only admins or moderators can delete arbitrary comments.';
    end if;

    delete from public.community_comments where id = p_comment_id;
end;
$$;

revoke all on function public.admin_delete_community_comment(uuid) from public;
grant execute on function public.admin_delete_community_comment(uuid) to anon, authenticated;

-- 14. RPC: Verify caller identity in create_community_post

drop function if exists public.create_community_post(text, text, boolean, boolean, integer, text, text, text, integer, text, text, text, text);
create or replace function public.create_community_post(
    p_author_uid text,
    p_author_username text,
    p_author_is_admin boolean,
    p_author_is_mod boolean,
    p_author_selected_avatar_id integer,
    p_author_custom_avatar_url text,
    p_car_model_name text,
    p_car_brand text,
    p_car_year integer,
    p_car_series text,
    p_car_image_url text,
    p_caption text,
    p_car_feature text
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
    v_caller_uid := public.request_uid();
    if v_caller_uid is null or v_caller_uid != p_author_uid then
        raise exception 'Identity mismatch: caller UID does not match p_author_uid.';
    end if;

    select is_admin, is_mod into p_author_is_admin, p_author_is_mod
    from public.profiles where firebase_uid = v_caller_uid;

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

-- 15. RPC: Verify caller identity in create_community_comment

drop function if exists public.create_community_comment(uuid, text, text, boolean, boolean, text);
create or replace function public.create_community_comment(
    p_post_id uuid,
    p_author_uid text,
    p_author_username text,
    p_author_is_admin boolean,
    p_author_is_mod boolean,
    p_text text
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
    v_caller_uid := public.request_uid();
    if v_caller_uid is null or v_caller_uid != p_author_uid then
        raise exception 'Identity mismatch: caller UID does not match p_author_uid.';
    end if;

    select is_admin, is_mod into p_author_is_admin, p_author_is_mod
    from public.profiles where firebase_uid = v_caller_uid;

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

-- 16. RPC: set_user_moderator - ensure identity check

drop function if exists public.set_user_moderator_by_firebase_uid(text, boolean);
create or replace function public.set_user_moderator_by_firebase_uid(
    target_firebase_uid text,
    mod_status boolean
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    if not exists (
        select 1 from public.profiles
        where firebase_uid = public.request_uid()
          and is_admin = true
    ) then
        raise exception 'Only admins can set moderator status';
    end if;
    update public.profiles set is_mod = mod_status where firebase_uid = target_firebase_uid;
end;
$$;

revoke all on function public.set_user_moderator_by_firebase_uid(text, boolean) from public;
grant execute on function public.set_user_moderator_by_firebase_uid(text, boolean) to anon, authenticated;

-- 17. PROFILES PUBLIC VIEW (email gizli - sadece kendi emailini gorebilir)

create or replace view public.profiles_public_view as
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

-- 18. RPC: delete_my_account_data - ensure identity check

drop function if exists public.delete_my_account_data(text);
create or replace function public.delete_my_account_data(p_uid text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    if public.request_uid() is null or public.request_uid() != p_uid then
        raise exception 'Identity mismatch: you can only delete your own account data.';
    end if;

    delete from public.community_comments where author_uid = p_uid;
    delete from public.community_likes where user_uid = p_uid;
    delete from public.follows where follower_uid = p_uid or followed_uid = p_uid;
    delete from public.banned_users where user_uid = p_uid or banned_by_uid = p_uid;
    delete from public.blocked_users where blocker_uid = p_uid or blocked_uid = p_uid;
    delete from public.community_posts where author_uid = p_uid;
    delete from public.messages where firebase_uid = p_uid or receiver_uid = p_uid;
    delete from public.user_collection_snapshots where firebase_uid = p_uid;
    delete from public.public_listings where firebase_uid = p_uid;
    delete from public.feedback_messages where firebase_uid = p_uid;
    delete from public.profiles where firebase_uid = p_uid;
end;
$$;

revoke all on function public.delete_my_account_data(text) from public;
grant execute on function public.delete_my_account_data(text) to anon, authenticated;

-- LOCKDOWN COMPLETE