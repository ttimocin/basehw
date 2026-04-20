-- Tighten notifications INSERT policy.
-- Prevent arbitrary spoofed notifications by enforcing caller identity.

drop policy if exists notifications_insert_auth on public.notifications;

create policy notifications_insert_auth
on public.notifications
for insert
to authenticated
with check (
    sender_uid = public.request_uid()
    and recipient_uid is not null
    and recipient_uid <> ''
);
