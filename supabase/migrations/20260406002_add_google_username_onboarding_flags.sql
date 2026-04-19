alter table if exists public.profiles
    add column if not exists google_username_onboarding_required boolean not null default true,
    add column if not exists google_username_onboarding_completed boolean not null default false;
