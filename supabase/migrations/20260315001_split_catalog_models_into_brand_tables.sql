begin;

-- One-time migration from legacy public.catalog_models to brand-specific tables.
-- Safe to re-run: uses ON CONFLICT upsert on (model_name, year, data_source).

-- If catalog_models was already archived/renamed (archive_legacy migration ran
-- first alphabetically), create an empty stub so the INSERT-SELECT below
-- produces 0 rows but succeeds cleanly.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT FROM pg_tables
        WHERE schemaname = 'public' AND tablename = 'catalog_models'
    ) THEN
        RAISE NOTICE 'catalog_models not found - creating empty stub for safe re-run.';
        CREATE TABLE public.catalog_models (
            model_name  text,
            brand       text,
            series      text,
            series_num  text,
            year        int,
            color       text,
            image_url   text,
            scale       text,
            toy_num     text,
            col_num     text,
            is_premium  bool,
            data_source text,
            case_num    text,
            updated_at  timestamptz
        );
    END IF;
END $$;

-- Ensure conflict targets exist even if table/index creation step was skipped earlier.
create unique index if not exists uq_catalog_hot_wheels_identity
  on public.catalog_hot_wheels(model_name, year, data_source);

create unique index if not exists uq_catalog_matchbox_identity
  on public.catalog_matchbox(model_name, year, data_source);

create unique index if not exists uq_catalog_mini_gt_identity
  on public.catalog_mini_gt(model_name, year, data_source);

create unique index if not exists uq_catalog_majorette_identity
  on public.catalog_majorette(model_name, year, data_source);

create unique index if not exists uq_catalog_jada_identity
  on public.catalog_jada(model_name, year, data_source);

insert into public.catalog_hot_wheels (
  model_name, series, series_num, year, color, image_url, scale,
  toy_num, col_num, is_premium, data_source, case_num, updated_at
)
select
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
  case
    when coalesce(data_source, '') = '' then 'hotwheels'
    else data_source
  end as data_source,
  case_num,
  updated_at
from public.catalog_models
where upper(brand) = 'HOT_WHEELS'
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
  updated_at = greatest(public.catalog_hot_wheels.updated_at, excluded.updated_at);

insert into public.catalog_matchbox (
  model_name, series, series_num, year, color, image_url, scale,
  toy_num, col_num, is_premium, data_source, case_num, updated_at
)
select
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
  case
    when coalesce(data_source, '') = '' then 'matchbox'
    else data_source
  end as data_source,
  case_num,
  updated_at
from public.catalog_models
where upper(brand) = 'MATCHBOX'
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
  updated_at = greatest(public.catalog_matchbox.updated_at, excluded.updated_at);

insert into public.catalog_mini_gt (
  model_name, series, series_num, year, color, image_url, scale,
  toy_num, col_num, is_premium, data_source, case_num, updated_at
)
select
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
  case
    when coalesce(data_source, '') = '' then 'minigt'
    else data_source
  end as data_source,
  case_num,
  updated_at
from public.catalog_models
where upper(brand) = 'MINI_GT'
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
  updated_at = greatest(public.catalog_mini_gt.updated_at, excluded.updated_at);

insert into public.catalog_majorette (
  model_name, series, series_num, year, color, image_url, scale,
  toy_num, col_num, is_premium, data_source, case_num, updated_at
)
select
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
  case
    when coalesce(data_source, '') = '' then 'majorette'
    else data_source
  end as data_source,
  case_num,
  updated_at
from public.catalog_models
where upper(brand) = 'MAJORETTE'
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
  updated_at = greatest(public.catalog_majorette.updated_at, excluded.updated_at);

insert into public.catalog_jada (
  model_name, series, series_num, year, color, image_url, scale,
  toy_num, col_num, is_premium, data_source, case_num, updated_at
)
select
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
  case
    when coalesce(data_source, '') = '' then 'jada'
    else data_source
  end as data_source,
  case_num,
  updated_at
from public.catalog_models
where upper(brand) = 'JADA'
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
  updated_at = greatest(public.catalog_jada.updated_at, excluded.updated_at);

commit;