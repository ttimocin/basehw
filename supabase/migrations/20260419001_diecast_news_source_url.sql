-- Dedupe ingest: one row per external article URL (multiple NULLs allowed for legacy rows)
alter table public.diecast_news
    add column if not exists source_url text;

create unique index if not exists idx_diecast_news_source_url_unique
    on public.diecast_news (source_url);
