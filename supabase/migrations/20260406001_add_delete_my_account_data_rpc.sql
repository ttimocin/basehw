-- Deletes all data that belongs to the current requester UID.
-- Intended to run before Firebase account deletion.

create or replace function public.delete_my_account_data(p_uid text default null)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid text;
begin
    v_uid := coalesce(p_uid, public.request_uid());
    if v_uid is null or v_uid = '' then
        raise exception 'UNAUTHORIZED';
    end if;

    -- Remove direct user-owned records
    delete from public.feedback_messages where firebase_uid = v_uid;
    delete from public.user_collection_snapshots where firebase_uid = v_uid;
    delete from public.public_listings where firebase_uid = v_uid;

    -- Remove DM rows where user is sender or receiver
    delete from public.messages
    where firebase_uid = v_uid or receiver_uid = v_uid;

    -- Social graph cleanup
    delete from public.follows
    where follower_uid = v_uid or followed_uid = v_uid;

    -- Optional tables (may not exist on all environments)
    if to_regclass('public.blocked_users') is not null then
        execute 'delete from public.blocked_users where blocker_uid = $1 or blocked_uid = $1' using v_uid;
    end if;

    if to_regclass('public.banned_users') is not null then
        execute 'delete from public.banned_users where user_uid = $1 or banned_by_uid = $1' using v_uid;
    end if;

    -- Community tables
    delete from public.community_likes where user_uid = v_uid;
    delete from public.community_comments where author_uid = v_uid;
    delete from public.community_posts where author_uid = v_uid;

    -- Finally remove profile (FK cascade covers dependent rows with FK refs)
    delete from public.profiles where firebase_uid = v_uid;
end;
$$;

revoke all on function public.delete_my_account_data(text) from public;
grant execute on function public.delete_my_account_data(text) to anon, authenticated;
