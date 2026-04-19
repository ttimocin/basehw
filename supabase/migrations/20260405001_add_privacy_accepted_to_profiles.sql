-- Separate login privacy acceptance from community rules acceptance.
alter table if exists public.profiles
    add column if not exists privacy_accepted boolean not null default false;
