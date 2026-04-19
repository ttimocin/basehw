-- Add avatar management fields to profiles table

alter table if exists public.profiles
    add column if not exists selected_avatar_id integer not null default 1,
    add column if not exists custom_avatar_url text;

comment on column public.profiles.selected_avatar_id is 'Avatar ID: 1-5 for default avatars, 0 for custom upload';
comment on column public.profiles.custom_avatar_url is 'Custom avatar URL from Supabase Storage (only used when selected_avatar_id = 0)';
