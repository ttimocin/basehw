-- Temporary compatibility policies for FirebaseAuth client without Supabase JWT claim mapping.
-- App authenticates with FirebaseAuth, not Supabase JWT sessions, so request_uid() is null.

drop policy if exists messages_select_participant on public.messages;
drop policy if exists messages_insert_sender on public.messages;

create policy messages_select_all
on public.messages
for select
to public
using (true);

create policy messages_insert_all
on public.messages
for insert
to public
with check (true);
