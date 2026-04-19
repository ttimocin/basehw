-- Avatar sistemi için profiles tablosuna sütun ekleme
-- Supabase SQL Editor'da çalıştırın

-- selected_avatar_id: 1-5 = varsayılan ikonlar, 0 = custom upload
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS selected_avatar_id INT DEFAULT 1;

-- custom_avatar_url: Kullanıcının galeriden yüklediği avatar URL'si
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS custom_avatar_url TEXT;

-- Mevcut kullanıcılara default avatar ata
UPDATE profiles SET selected_avatar_id = 1 WHERE selected_avatar_id IS NULL;