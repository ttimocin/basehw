-- Temporary compatibility policy for FirebaseAuth client without Supabase JWT claim mapping.
-- Ensure follows graph can be read by Android client even when request_uid() is null.

drop policy if exists follows_select_participant on public.follows;

create policy follows_select_all
on public.follows
for select
to public
using (true);
