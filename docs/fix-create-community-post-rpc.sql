-- FIX: create_community_post RPC function parameter mismatch
-- Run this in Supabase Dashboard -> SQL Editor
-- Date: 2026-04-10

-- First, add missing columns to community_posts table if they don't exist
ALTER TABLE community_posts 
ADD COLUMN IF NOT EXISTS author_is_mod BOOLEAN DEFAULT FALSE;

ALTER TABLE community_posts 
ADD COLUMN IF NOT EXISTS author_selected_avatar_id INTEGER DEFAULT 1;

ALTER TABLE community_posts 
ADD COLUMN IF NOT EXISTS author_custom_avatar_url TEXT;

ALTER TABLE community_posts 
ADD COLUMN IF NOT EXISTS car_feature TEXT;

-- Drop old function (all overloads)
DROP FUNCTION IF EXISTS create_community_post(TEXT, TEXT, BOOLEAN, TEXT, TEXT, TEXT, TEXT, TEXT, TEXT);
DROP FUNCTION IF EXISTS create_community_post(TEXT, TEXT, BOOLEAN, BOOLEAN, TEXT, TEXT, TEXT, TEXT, TEXT);
DROP FUNCTION IF EXISTS create_community_post;

-- Create updated function with all parameters the app sends
CREATE OR REPLACE FUNCTION create_community_post(
    p_author_uid TEXT,
    p_author_username TEXT,
    p_author_is_admin BOOLEAN DEFAULT FALSE,
    p_author_is_mod BOOLEAN DEFAULT FALSE,
    p_author_selected_avatar_id INTEGER DEFAULT 1,
    p_author_custom_avatar_url TEXT DEFAULT NULL,
    p_car_model_name TEXT DEFAULT '',
    p_car_brand TEXT DEFAULT '',
    p_car_year INTEGER DEFAULT NULL,
    p_car_series TEXT DEFAULT NULL,
    p_car_image_url TEXT DEFAULT '',
    p_caption TEXT DEFAULT '',
    p_car_feature TEXT DEFAULT NULL
)
RETURNS TABLE(id UUID) AS $$
BEGIN
    RETURN QUERY
    INSERT INTO community_posts (
        author_uid,
        author_username,
        author_is_admin,
        author_is_mod,
        author_selected_avatar_id,
        author_custom_avatar_url,
        car_model_name,
        car_brand,
        car_year,
        car_series,
        car_image_url,
        caption,
        car_feature,
        created_at,
        is_active
    ) VALUES (
        p_author_uid,
        p_author_username,
        p_author_is_admin,
        p_author_is_mod,
        p_author_selected_avatar_id,
        p_author_custom_avatar_url,
        p_car_model_name,
        p_car_brand,
        p_car_year,
        p_car_series,
        p_car_image_url,
        p_caption,
        p_car_feature,
        NOW(),
        TRUE
    )
    RETURNING community_posts.id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION create_community_post TO authenticated;
GRANT EXECUTE ON FUNCTION create_community_post TO anon;