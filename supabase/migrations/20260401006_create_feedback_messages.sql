create table if not exists public.feedback_messages (
    id uuid primary key default gen_random_uuid(),
    firebase_uid text not null,
    username text not null,
    subject text not null,
    message text not null,
    created_at timestamptz not null default now()
);

create index if not exists idx_feedback_messages_firebase_uid
    on public.feedback_messages (firebase_uid, created_at desc);

alter table public.feedback_messages enable row level security;

drop policy if exists feedback_messages_insert_owner on public.feedback_messages;
create policy feedback_messages_insert_owner
on public.feedback_messages
for insert
to public
with check (firebase_uid = public.request_uid());

drop policy if exists feedback_messages_select_owner on public.feedback_messages;
create policy feedback_messages_select_owner
on public.feedback_messages
for select
to public
using (firebase_uid = public.request_uid());
