-- Migration: Replace sth and chase with feature column in all catalog tables
-- Date: 2026-03-17

DO $$
DECLARE
    table_name_text text;
    catalog_tables text[] := ARRAY[
        'catalog_hot_wheels',
        'catalog_matchbox',
        'catalog_mini_gt',
        'catalog_majorette',
        'catalog_jada',
        'catalog_siku'
    ];
BEGIN
    FOREACH table_name_text IN ARRAY catalog_tables
    LOOP
        -- Add feature column if it doesn't exist
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = table_name_text AND column_name = 'feature') THEN
            EXECUTE format('ALTER TABLE %I ADD COLUMN feature TEXT DEFAULT NULL', table_name_text);
        END IF;

        -- Migrate data from sth/chase to feature if columns exist
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = table_name_text AND column_name = 'sth') THEN
            EXECUTE format('UPDATE %I SET feature = ''sth'' WHERE sth = ''yes'' OR sth = ''true''', table_name_text);
        END IF;

        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = table_name_text AND column_name = 'chase') THEN
            EXECUTE format('UPDATE %I SET feature = ''chase'' WHERE (chase = ''yes'' OR chase = ''true'') AND feature IS NULL', table_name_text);
        END IF;

        -- Remove old columns
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = table_name_text AND column_name = 'sth') THEN
            EXECUTE format('ALTER TABLE %I DROP COLUMN sth', table_name_text);
        END IF;

        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = table_name_text AND column_name = 'chase') THEN
            EXECUTE format('ALTER TABLE %I DROP COLUMN chase', table_name_text);
        END IF;
    END LOOP;
END $$;
