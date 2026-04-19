-- Admin Panel RPC Functions
-- Run this in Supabase Dashboard -> SQL Editor
-- Date: 2026-04-10

-- ============================================================
-- ADMIN RPC FUNCTIONS (SECURITY DEFINER)
-- Bu fonksiyonlar Firebase UID ile admin kontrolü yapar
-- ============================================================

-- Admin için tüm profilleri getirme fonksiyonu
CREATE OR REPLACE FUNCTION admin_get_all_profiles(p_admin_uid TEXT)
RETURNS SETOF profiles AS $$
DECLARE
    v_is_admin BOOLEAN;
    v_is_mod BOOLEAN;
BEGIN
    SELECT is_admin, is_mod INTO v_is_admin, v_is_mod 
    FROM profiles WHERE firebase_uid = p_admin_uid;
    
    IF NOT (v_is_admin = true OR v_is_mod = true) THEN
        RAISE EXCEPTION 'Admin veya Mod yetkisi gerekli';
    END IF;
    
    RETURN QUERY SELECT * FROM profiles ORDER BY created_at DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Admin için kullanıcı banlama fonksiyonu
CREATE OR REPLACE FUNCTION admin_ban_user(
    p_admin_uid TEXT,
    p_target_uid TEXT,
    p_reason TEXT DEFAULT NULL
)
RETURNS VOID AS $$
DECLARE
    v_is_admin BOOLEAN;
    v_is_mod BOOLEAN;
    v_target_is_admin BOOLEAN;
    v_target_is_mod BOOLEAN;
BEGIN
    SELECT is_admin, is_mod INTO v_is_admin, v_is_mod 
    FROM profiles WHERE firebase_uid = p_admin_uid;
    
    IF NOT (v_is_admin = true OR v_is_mod = true) THEN
        RAISE EXCEPTION 'Admin veya Mod yetkisi gerekli';
    END IF;
    
    -- Mod, başka mod veya admin'i banlayamaz
    IF v_is_mod = true AND v_is_admin = false THEN
        SELECT is_admin, is_mod INTO v_target_is_admin, v_target_is_mod 
        FROM profiles WHERE firebase_uid = p_target_uid;
        
        IF v_target_is_admin = true OR v_target_is_mod = true THEN
            RAISE EXCEPTION 'Moderatörler başka moderatör veya admini banlayamaz';
        END IF;
    END IF;
    
    INSERT INTO banned_users (user_uid, banned_by_uid, reason)
    VALUES (p_target_uid, p_admin_uid, p_reason)
    ON CONFLICT (user_uid) DO UPDATE SET 
        banned_by_uid = EXCLUDED.banned_by_uid,
        reason = EXCLUDED.reason,
        created_at = NOW();
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Admin için ban kaldırma fonksiyonu
CREATE OR REPLACE FUNCTION admin_unban_user(p_admin_uid TEXT, p_target_uid TEXT)
RETURNS VOID AS $$
DECLARE
    v_is_admin BOOLEAN;
    v_is_mod BOOLEAN;
BEGIN
    SELECT is_admin, is_mod INTO v_is_admin, v_is_mod 
    FROM profiles WHERE firebase_uid = p_admin_uid;
    
    IF NOT (v_is_admin = true OR v_is_mod = true) THEN
        RAISE EXCEPTION 'Admin veya Mod yetkisi gerekli';
    END IF;
    
    DELETE FROM banned_users WHERE user_uid = p_target_uid;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Admin için banlanan kullanıcıları getirme fonksiyonu
CREATE OR REPLACE FUNCTION admin_get_banned_users(p_admin_uid TEXT)
RETURNS TABLE(
    user_uid TEXT,
    banned_by_uid TEXT,
    reason TEXT,
    created_at TIMESTAMPTZ
) AS $$
DECLARE
    v_is_admin BOOLEAN;
    v_is_mod BOOLEAN;
BEGIN
    SELECT is_admin, is_mod INTO v_is_admin, v_is_mod 
    FROM profiles WHERE firebase_uid = p_admin_uid;
    
    IF NOT (v_is_admin = true OR v_is_mod = true) THEN
        RAISE EXCEPTION 'Admin veya Mod yetkisi gerekli';
    END IF;
    
    RETURN QUERY SELECT b.user_uid, b.banned_by_uid, b.reason, b.created_at 
    FROM banned_users b ORDER BY b.created_at DESC;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Admin için post silme fonksiyonu
CREATE OR REPLACE FUNCTION admin_delete_post(p_admin_uid TEXT, p_post_id UUID)
RETURNS VOID AS $$
DECLARE
    v_is_admin BOOLEAN;
    v_is_mod BOOLEAN;
BEGIN
    SELECT is_admin, is_mod INTO v_is_admin, v_is_mod 
    FROM profiles WHERE firebase_uid = p_admin_uid;
    
    IF NOT (v_is_admin = true OR v_is_mod = true) THEN
        RAISE EXCEPTION 'Admin veya Mod yetkisi gerekli';
    END IF;
    
    UPDATE community_posts SET is_active = false WHERE id = p_post_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Admin için yorum silme fonksiyonu
CREATE OR REPLACE FUNCTION admin_delete_comment(p_admin_uid TEXT, p_comment_id UUID)
RETURNS VOID AS $$
DECLARE
    v_is_admin BOOLEAN;
    v_is_mod BOOLEAN;
BEGIN
    SELECT is_admin, is_mod INTO v_is_admin, v_is_mod 
    FROM profiles WHERE firebase_uid = p_admin_uid;
    
    IF NOT (v_is_admin = true OR v_is_mod = true) THEN
        RAISE EXCEPTION 'Admin veya Mod yetkisi gerekli';
    END IF;
    
    DELETE FROM community_comments WHERE id = p_comment_id;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- İzinler
GRANT EXECUTE ON FUNCTION admin_get_all_profiles TO authenticated, anon;
GRANT EXECUTE ON FUNCTION admin_ban_user TO authenticated, anon;
GRANT EXECUTE ON FUNCTION admin_unban_user TO authenticated, anon;
GRANT EXECUTE ON FUNCTION admin_get_banned_users TO authenticated, anon;
GRANT EXECUTE ON FUNCTION admin_delete_post TO authenticated, anon;
GRANT EXECUTE ON FUNCTION admin_delete_comment TO authenticated, anon;