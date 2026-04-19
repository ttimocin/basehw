-- ===============================================================
-- SUPABASE FULL USER DATA RESET (CLEAN SLATE)
-- ===============================================================
-- DİKKAT: Bu işlem tüm kullanıcı profillerini, mesajları, 
-- koleksiyon yedeklerini ve forum verilerini kalıcı olarak siler.
-- SADECE 'master_data' (Ana Katalog) korunur.
-- ===============================================================

BEGIN;

-- TRUNCATE ile tabloları boşaltıyoruz. 
-- CASCADE seçeneği, tablolar arası ilişkileri takip ederek bağlı verileri de temizler.
TRUNCATE TABLE 
    public.community_comments,
    public.community_posts,
    public.messages,
    public.follows,
    public.public_listings,
    public.banned_users,
    public.user_collection_snapshots,
    public.profiles
RESTART IDENTITY CASCADE;

-- Master data tablosunun silinmediğinden emin olmak için işlem sonunda kontrol edilebilir.
-- SELECT COUNT(*) FROM public.master_data;

COMMIT;

-- ===============================================================
-- İŞLEM TAMAMLANDI. 
-- Uygulamayı kullanan kullanıcıların tekrar giriş yapması 
-- ve yeni profil oluşturması gerekecektir.
-- ===============================================================
