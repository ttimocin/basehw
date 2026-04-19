-- Cleanup: Remove old/duplicate "open" policies that were left behind
-- These are dangerous policies like "using (true)" or "with check (true)"
-- that allow anyone to do anything.

-- banned_users: remove old open policies
drop policy if exists "banned_users_delete" on public.banned_users;
drop policy if exists "banned_users_insert" on public.banned_users;

-- community_comments: remove old open policies
drop policy if exists "comments_read_all" on public.community_comments;
drop policy if exists "comments_self_delete" on public.community_comments;

-- community_posts: remove old open policies
drop policy if exists "posts_read_all" on public.community_posts;
drop policy if exists "posts_self_manage" on public.community_posts;

-- feedback_messages: remove old duplicate
drop policy if exists "feedback_messages_select_owner" on public.feedback_messages;

-- messages: remove old open policies
drop policy if exists "messages_insert_all" on public.messages;
drop policy if exists "messages_insert_self" on public.messages;
drop policy if exists "messages_select_all" on public.messages;

-- public_listings: remove old open policies
drop policy if exists "listings_delete_all" on public.public_listings;
drop policy if exists "listings_insert_all" on public.public_listings;
drop policy if exists "listings_select_visible" on public.public_listings;
drop policy if exists "listings_update_all" on public.public_listings;

-- user_collection_snapshots: remove old open policies
drop policy if exists "collection_snapshots_delete_all" on public.user_collection_snapshots;
drop policy if exists "collection_snapshots_insert_all" on public.user_collection_snapshots;
drop policy if exists "collection_snapshots_select_all" on public.user_collection_snapshots;
drop policy if exists "collection_snapshots_update_all" on public.user_collection_snapshots;

-- Cleanup complete