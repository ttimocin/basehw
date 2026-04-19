-- Add case-insensitive username uniqueness support on profiles.

alter table if exists public.profiles
    add column if not exists username_lower text;

update public.profiles
set username_lower = lower(display_name)
where username_lower is null
  and display_name is not null;

with ranked as (
    select
        firebase_uid,
        username_lower,
        row_number() over (
            partition by username_lower
            order by created_at asc, firebase_uid asc
        ) as rn
    from public.profiles
    where username_lower is not null
)
update public.profiles p
set username_lower = null
from ranked r
where p.firebase_uid = r.firebase_uid
  and r.rn > 1;

create unique index if not exists uq_profiles_username_lower
    on public.profiles (username_lower)
    where username_lower is not null;
