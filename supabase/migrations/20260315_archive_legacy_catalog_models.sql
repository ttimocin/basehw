begin;

-- Optional maintenance step after successful migration verification.
-- Purpose:
-- 1) keep old data as backup
-- 2) prevent accidental reads/writes to legacy table name

-- If legacy table does not exist, do nothing.
do $$
begin
  if to_regclass('public.catalog_models') is not null then
    -- Move old table to an archive name (one-time).
    if to_regclass('public.catalog_models_legacy_archive') is null then
      execute 'alter table public.catalog_models rename to catalog_models_legacy_archive';
    end if;

    -- Revoke public permissions from archived table.
    execute 'revoke all on table public.catalog_models_legacy_archive from anon';
    execute 'revoke all on table public.catalog_models_legacy_archive from authenticated';
  end if;
end $$;

commit;
