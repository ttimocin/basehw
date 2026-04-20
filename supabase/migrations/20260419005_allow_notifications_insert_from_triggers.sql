-- Keep anti-spoofing checks for direct client inserts,
-- while allowing trusted trigger-based inserts.

drop policy if exists notifications_insert_auth on public.notifications;

create policy notifications_insert_auth
on public.notifications
for insert
to public
with check (
    recipient_uid is not null
    and recipient_uid <> ''
    and (
        -- Trigger path (e.g. follows -> handle_new_follow_notification)
        pg_trigger_depth() > 0
        or
        -- Direct client path: sender must match caller identity
        sender_uid = public.request_uid()
    )
);
