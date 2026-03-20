-- Migration to add scale, sth, and chase columns to all catalog tables.
-- This ensures the sync mechanism can handle these special flags.

begin;

-- List of tables to update
-- catalog_hot_wheels, catalog_matchbox, catalog_mini_gt, catalog_majorette, catalog_jada, catalog_siku

DO $$
DECLARE
    t text;
    tables text[] := ARRAY['catalog_hot_wheels', 'catalog_matchbox', 'catalog_mini_gt', 'catalog_majorette', 'catalog_jada', 'catalog_siku'];
BEGIN
    FOREACH t IN ARRAY tables LOOP
        -- Add scale if missing (it might be there in some tables based on previous migrations)
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = t AND column_name = 'scale') THEN
            EXECUTE format('ALTER TABLE public.%I ADD COLUMN scale text NOT NULL DEFAULT ''1:64''', t);
        END IF;

        -- Add sth column
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = t AND column_name = 'sth') THEN
            EXECUTE format('ALTER TABLE public.%I ADD COLUMN sth text', t);
        END IF;

        -- Add chase column
        IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = t AND column_name = 'chase') THEN
            EXECUTE format('ALTER TABLE public.%I ADD COLUMN chase text', t);
        END IF;
    END LOOP;
END $$;

commit;
