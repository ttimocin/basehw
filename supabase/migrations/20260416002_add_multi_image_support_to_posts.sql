-- Support multiple photos in forum posts
-- Step 1: Add the column to the table
alter table public.community_posts
add column if not exists car_image_urls text[] default array[]::text[];

-- Step 2: Update the create_community_post RPC to handle the new field
drop function if exists public.create_community_post(text, text, boolean, boolean, integer, text, text, text, integer, text, text, text, text);

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
    p_car_feature text default null,
    p_car_image_urls text[] default array[]::text[]
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
        is_active,
        car_image_urls
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
        true,
        p_car_image_urls
    )
    returning community_posts.id;
end;
$$;

grant execute on function public.create_community_post(text, text, boolean, boolean, integer, text, text, text, integer, text, text, text, text, text[]) to anon, authenticated;

notify pgrst, 'reload schema';
