-- Temporary compatibility policies for Firebase-auth-only Android client.
-- App authenticates with FirebaseAuth, not Supabase JWT sessions, so request_uid() is null.

-- profiles write policies

drop policy if exists profiles_update_own on public.profiles;
create policy profiles_update_all
on public.profiles
for update
to public
using (true)
with check (true);

drop policy if exists profiles_insert_own on public.profiles;
create policy profiles_insert_all
on public.profiles
for insert
to public
with check (true);

-- community posts/likes/comments write policies

drop policy if exists community_posts_insert_owner on public.community_posts;
drop policy if exists community_posts_update_owner on public.community_posts;
drop policy if exists community_posts_delete_owner on public.community_posts;
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

drop policy if exists community_likes_insert_owner on public.community_likes;
drop policy if exists community_likes_delete_owner on public.community_likes;
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

drop policy if exists community_comments_insert_owner on public.community_comments;
drop policy if exists community_comments_delete_owner_or_post_owner on public.community_comments;
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

-- follows writes

drop policy if exists follows_insert_owner on public.follows;
drop policy if exists follows_delete_owner on public.follows;
create policy follows_insert_all
on public.follows
for insert
to public
with check (true);
create policy follows_delete_all
on public.follows
for delete
to public
using (true);

-- feedback writes

drop policy if exists feedback_messages_insert_owner on public.feedback_messages;
create policy feedback_messages_insert_all
on public.feedback_messages
for insert
to public
with check (true);
