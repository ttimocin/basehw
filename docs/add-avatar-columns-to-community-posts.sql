-- Add avatar columns to community_posts table
-- Run this in Supabase SQL Editor

-- Add author_selected_avatar_id column (default 1 for existing posts)
ALTER TABLE community_posts 
ADD COLUMN IF NOT EXISTS author_selected_avatar_id INTEGER DEFAULT 1;

-- Add author_custom_avatar_url column (nullable for existing posts)
ALTER TABLE community_posts 
ADD COLUMN IF NOT EXISTS author_custom_avatar_url TEXT;

-- Add comment for documentation
COMMENT ON COLUMN community_posts.author_selected_avatar_id IS 'Selected avatar ID (1-5 for default avatars, 0 for custom upload)';
COMMENT ON COLUMN community_posts.author_custom_avatar_url IS 'URL of custom uploaded avatar image';