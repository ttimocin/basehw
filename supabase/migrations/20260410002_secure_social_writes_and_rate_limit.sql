-- Enforce social writes through RPC only and add DB-side rate limiting.
-- Direct INSERT on community_posts/community_comments is blocked by trigger.

create or replace function public.enforce_social_write_via_rpc()
returns trigger
language plpgsql
as $$
begin
    if current_setting('app.social_write_via_rpc', true) = '1' then
        return new;
    end if;

    if current_user in ('postgres', 'service_role') then
        return new;
    end if;

    raise exception 'Direct insert is disabled. Use RPC write functions.';
end;
$$;

drop trigger if exists trg_enforce_social_posts_rpc_only on public.community_posts;
create trigger trg_enforce_social_posts_rpc_only
before insert on public.community_posts
for each row
execute function public.enforce_social_write_via_rpc();

drop trigger if exists trg_enforce_social_comments_rpc_only on public.community_comments;
create trigger trg_enforce_social_comments_rpc_only
before insert on public.community_comments
for each row
execute function public.enforce_social_write_via_rpc();

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
begin
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
begin
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

revoke all on function public.create_community_post(text, text, boolean, boolean, integer, text, text, text, integer, text, text, text, text) from public;
grant execute on function public.create_community_post(text, text, boolean, boolean, integer, text, text, text, integer, text, text, text, text) to anon, authenticated;

revoke all on function public.create_community_comment(uuid, text, text, boolean, boolean, text) from public;
grant execute on function public.create_community_comment(uuid, text, text, boolean, boolean, text) to anon, authenticated;
