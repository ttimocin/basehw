-- Reset social and collection data for clean end-to-end testing.
-- WARNING: This is destructive and clears existing app data in remote DB.

begin;

truncate table public.community_likes restart identity cascade;
truncate table public.community_comments restart identity cascade;
truncate table public.community_posts restart identity cascade;
truncate table public.messages restart identity cascade;
truncate table public.public_listings restart identity cascade;
truncate table public.user_collection_snapshots restart identity cascade;
truncate table public.follows restart identity cascade;

update public.profiles
set
    follower_count = 0,
    following_count = 0,
    post_count = 0;

commit;
