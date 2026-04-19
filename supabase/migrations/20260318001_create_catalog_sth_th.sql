-- Add STH TH catalog table for brand-split sync model.

create table if not exists public.catalog_sth_th (
  id bigserial primary key,
  model_name text not null,
  series text not null default '',
  series_num text not null default '',
  year text,
  color text not null default '',
  image_url text not null default '',
  scale text not null default '1:64',
  toy_num text not null default '',
  col_num text not null default '',
  is_premium boolean not null default false,
  data_source text not null default 'hotwheels_th_sth',
  case_num text not null default '',
  feature text default null,
  updated_at timestamptz not null default now()
);

create index if not exists idx_catalog_sth_th_updated_at
  on public.catalog_sth_th(updated_at desc);

create unique index if not exists uq_catalog_sth_th_identity
  on public.catalog_sth_th(model_name, year, data_source);

create unique index if not exists uq_catalog_sth_th_toynum_source
  on public.catalog_sth_th(toy_num, data_source)
  where toy_num <> '';

alter table public.catalog_sth_th enable row level security;

drop policy if exists catalog_read_all on public.catalog_sth_th;
create policy catalog_read_all on public.catalog_sth_th
for select
using (true);

drop trigger if exists trg_catalog_sth_th_touch_updated_at on public.catalog_sth_th;
create trigger trg_catalog_sth_th_touch_updated_at
before update on public.catalog_sth_th
for each row execute function public.touch_updated_at();
