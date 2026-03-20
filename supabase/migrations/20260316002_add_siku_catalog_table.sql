-- Add SIKU catalog table for brand-split sync model.
-- Safe to run multiple times.

create table if not exists public.catalog_siku (
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
  data_source text not null default 'siku',
  case_num text not null default '',
  updated_at timestamptz not null default now()
);

create index if not exists idx_catalog_siku_updated_at
  on public.catalog_siku(updated_at desc);

create unique index if not exists uq_catalog_siku_identity
  on public.catalog_siku(model_name, year, data_source);

create unique index if not exists uq_catalog_siku_toynum_source
  on public.catalog_siku(toy_num, data_source)
  where toy_num <> '';

alter table public.catalog_siku enable row level security;

drop policy if exists catalog_read_all on public.catalog_siku;
create policy catalog_read_all on public.catalog_siku
for select
using (true);

create or replace function public.touch_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_catalog_siku_touch_updated_at on public.catalog_siku;
create trigger trg_catalog_siku_touch_updated_at
before update on public.catalog_siku
for each row execute function public.touch_updated_at();

-- Optional one-time backfill if legacy catalog_models still exists.
do $$
begin
  if to_regclass('public.catalog_models') is not null then
    insert into public.catalog_siku (
      model_name,
      series,
      series_num,
      year,
      color,
      image_url,
      scale,
      toy_num,
      col_num,
      is_premium,
      data_source,
      case_num,
      updated_at
    )
    select
      coalesce(model_name, ''),
      coalesce(series, ''),
      coalesce(series_num, ''),
      nullif(trim(year::text), ''),
      coalesce(color, ''),
      coalesce(image_url, ''),
      coalesce(scale, '1:64'),
      coalesce(toy_num, ''),
      coalesce(col_num, ''),
      coalesce(is_premium, false),
      coalesce(nullif(data_source, ''), 'siku'),
      coalesce(case_num, ''),
      coalesce(updated_at, now())
    from public.catalog_models
    where upper(coalesce(brand, '')) = 'SIKU'
    on conflict (model_name, year, data_source)
    do update set
      series = excluded.series,
      series_num = excluded.series_num,
      color = excluded.color,
      image_url = excluded.image_url,
      scale = excluded.scale,
      toy_num = excluded.toy_num,
      col_num = excluded.col_num,
      is_premium = excluded.is_premium,
      case_num = excluded.case_num,
      updated_at = greatest(public.catalog_siku.updated_at, excluded.updated_at);
  end if;
end $$;
