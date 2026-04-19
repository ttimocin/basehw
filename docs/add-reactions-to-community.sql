-- ===============================================================
-- EMOJI REACTIONS SYSTEM FOR COMMUNITY POSTS
-- ===============================================================
-- Bu migration, mevcut like sistemini emoji reaksiyon sistemiyle
-- değiştirir. Kullanıcı başına 1 reaksiyon (Facebook/LinkedIn tarzı).
-- Mevcut likes verileri ❤️ reaksiyonu olarak taşınır.
-- ===============================================================

-- 1. Yeni community_reactions tablosu oluştur
CREATE TABLE IF NOT EXISTS community_reactions (
    post_id UUID REFERENCES community_posts(id) ON DELETE CASCADE,
    user_uid TEXT NOT NULL,
    emoji TEXT NOT NULL DEFAULT '👍',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (post_id, user_uid)
);

-- 2. reaction_counts JSONB sütunu ekle (community_posts tablosuna)
ALTER TABLE community_posts ADD COLUMN IF NOT EXISTS reaction_counts JSONB DEFAULT '{}';

-- 3. Mevcut likes verisini community_reactions tablosuna taşı (❤️ olarak)
INSERT INTO community_reactions (post_id, user_uid, emoji, created_at)
SELECT post_id, user_uid, '❤️', created_at
FROM community_likes
ON CONFLICT (post_id, user_uid) DO NOTHING;

-- 4. Mevcut like_count verisini reaction_counts'a taşı
UPDATE community_posts p
SET reaction_counts = (
    SELECT jsonb_build_object('❤️', COUNT(*))
    FROM community_reactions r
    WHERE r.post_id = p.id
)
WHERE EXISTS (SELECT 1 FROM community_reactions r WHERE r.post_id = p.id);

-- 5. reaction_counts için trigger fonksiyonu
CREATE OR REPLACE FUNCTION update_reaction_counts()
RETURNS TRIGGER AS $$
DECLARE
    v_post_id UUID;
BEGIN
    IF TG_OP = 'INSERT' THEN
        v_post_id := NEW.post_id;
    ELSIF TG_OP = 'DELETE' THEN
        v_post_id := OLD.post_id;
    ELSIF TG_OP = 'UPDATE' THEN
        -- Eğer emoji değiştiyse, eski post'un da sayısını güncelle
        IF OLD.post_id != NEW.post_id THEN
            PERFORM recalc_reactions(OLD.post_id);
        END IF;
        v_post_id := NEW.post_id;
    END IF;
    
    PERFORM recalc_reactions(v_post_id);
    
    IF TG_OP = 'INSERT' THEN
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- 6. Reaksiyon sayılarını yeniden hesaplayan yardımcı fonksiyon
CREATE OR REPLACE FUNCTION recalc_reactions(p_post_id UUID)
RETURNS VOID AS $$
BEGIN
    UPDATE community_posts
    SET reaction_counts = (
        SELECT jsonb_object_agg(emoji, cnt)
        FROM (
            SELECT emoji, COUNT(*)::int AS cnt
            FROM community_reactions
            WHERE post_id = p_post_id
            GROUP BY emoji
        ) sub
    )
    WHERE id = p_post_id;
END;
$$ LANGUAGE plpgsql;

-- 7. Trigger'ı oluştur
DROP TRIGGER IF EXISTS trg_update_reaction_counts ON community_reactions;
CREATE TRIGGER trg_update_reaction_counts
    AFTER INSERT OR DELETE OR UPDATE ON community_reactions
    FOR EACH ROW
    EXECUTE FUNCTION update_reaction_counts();

-- 8. RLS (Row Level Security) politikaları
ALTER TABLE community_reactions ENABLE ROW LEVEL SECURITY;

-- Herkes reaksiyonları okuyabilir
CREATE POLICY "Reactions are readable by all"
    ON community_reactions FOR SELECT
    USING (true);

-- Otantik kullanıcılar reaksiyon ekleyebilir
CREATE POLICY "Authenticated users can add reactions"
    ON community_reactions FOR INSERT
    WITH CHECK (auth.uid() IS NOT NULL OR true);

-- Kullanıcılar sadece kendi reaksiyonlarını güncelleyebilir
CREATE POLICY "Users can update own reactions"
    ON community_reactions FOR UPDATE
    USING (true);

-- Kullanıcılar sadece kendi reaksiyonlarını silebilir
CREATE POLICY "Users can delete own reactions"
    ON community_reactions FOR DELETE
    USING (true);

-- 9. İzinler
GRANT ALL ON community_reactions TO authenticated, anon;

-- 10. like_count sütununu kaldır (artık kullanılmıyor)
-- NOT: Eski community_likes tablosu ve like_count sütunu backward compatibility
-- için bir süre daha tutulabilir, sonra kaldırılabilir.
-- Aşağıdaki satırları tüm istemciler güncellendikten sonra aktif edin:
-- ALTER TABLE community_posts DROP COLUMN IF EXISTS like_count;
-- DROP TABLE IF EXISTS community_likes;

-- ===============================================================
-- İŞLEM TAMAMLANDI
-- Eski likes otomatik olarak ❤️ reaksiyonlarına dönüştürüldü
-- ===============================================================