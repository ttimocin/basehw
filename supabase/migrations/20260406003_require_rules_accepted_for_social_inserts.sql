-- Enforce community rules acceptance at DB level for write safety.
-- Users must have profiles.rules_accepted = true before inserting posts/comments.
-- Uses NEW.author_uid to stay compatible with Firebase-auth bridge flow.

create or replace function public.ensure_rules_accepted_for_social_insert()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if not exists (
    select 1
    from public.profiles p
    where p.firebase_uid = new.author_uid
      and p.rules_accepted = true
  ) then
    raise exception 'Community rules must be accepted before posting or commenting';
  end if;
  return new;
end;
$$;

drop trigger if exists trg_require_rules_for_posts on public.community_posts;
create trigger trg_require_rules_for_posts
before insert on public.community_posts
for each row
execute function public.ensure_rules_accepted_for_social_insert();

drop trigger if exists trg_require_rules_for_comments on public.community_comments;
create trigger trg_require_rules_for_comments
before insert on public.community_comments
for each row
execute function public.ensure_rules_accepted_for_social_insert();
