-- Critical fix: prevent IDOR in delete_my_account_data RPC.
-- Enforce caller identity and restrict execute permission to authenticated users.

create or replace function public.delete_my_account_data(p_uid text default null)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    v_request_uid text;
    v_uid text;
begin
    v_request_uid := public.request_uid();
    if v_request_uid is null or v_request_uid = '' then
        raise exception 'UNAUTHORIZED';
    end if;

    -- If client sends p_uid, it must match caller identity.
    if p_uid is not null and p_uid <> v_request_uid then
        raise exception 'UNAUTHORIZED';
    end if;

    v_uid := v_request_uid;

    -- Remove direct user-owned records
    delete from public.feedback_messages where firebase_uid = v_uid;
    delete from public.user_collection_snapshots where firebase_uid = v_uid;
    delete from public.public_listings where firebase_uid = v_uid;
    delete from public.notifications where recipient_uid = v_uid or sender_uid = v_uid;

    -- Remove DM rows where user is sender or receiver
    delete from public.messages
    where firebase_uid = v_uid or receiver_uid = v_uid;

    -- Optional conversation summaries table (if any environment has it)
    if to_regclass('public.direct_conversations') is not null then
        execute 'delete from public.direct_conversations where user_a_uid = $1 or user_b_uid = $1' using v_uid;
    end if;

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
    if to_regclass('public.community_reactions') is not null then
        execute 'delete from public.community_reactions where user_uid = $1' using v_uid;
    end if;
    delete from public.community_comments where author_uid = v_uid;
    delete from public.community_posts where author_uid = v_uid;

    -- Finally remove profile
    delete from public.profiles where firebase_uid = v_uid;
end;
$$;

revoke all on function public.delete_my_account_data(text) from public;
grant execute on function public.delete_my_account_data(text) to authenticated;
