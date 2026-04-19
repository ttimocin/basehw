-- Harden social RLS policies by binding writes to request identity.
-- Identity source: JWT sub claim (Firebase UID bridge).

create or replace function public.request_uid()
returns text
language sql
stable
as $$
    select nullif(
        coalesce(
            auth.jwt() ->> 'sub',
            auth.uid()::text,
            current_setting('request.jwt.claim.sub', true)
        ),
        ''
    )
$$;

grant execute on function public.request_uid() to anon, authenticated;

-- community_posts

drop policy if exists community_posts_select_all on public.community_posts;
drop policy if exists community_posts_insert_all on public.community_posts;
drop policy if exists community_posts_update_all on public.community_posts;
drop policy if exists community_posts_delete_all on public.community_posts;

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

-- community_likes

drop policy if exists community_likes_select_all on public.community_likes;
drop policy if exists community_likes_insert_all on public.community_likes;
drop policy if exists community_likes_delete_all on public.community_likes;

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

-- community_comments

drop policy if exists community_comments_select_all on public.community_comments;
drop policy if exists community_comments_insert_all on public.community_comments;
drop policy if exists community_comments_delete_all on public.community_comments;

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

-- messages

drop policy if exists messages_select_participant on public.messages;
drop policy if exists messages_insert_sender on public.messages;

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
