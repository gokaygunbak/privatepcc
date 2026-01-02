-- Topics tablosunu güncelleme ve yeni veriler ekleme
-- Bu SQL'i PostgreSQL'de çalıştırın
-- ⚠️ DİKKAT: Bu script mevcut verileri SİLMEZ, sadece günceller/ekler

-- 1. keywords kolonunu kaldır (varsa)
ALTER TABLE public.topics DROP COLUMN IF EXISTS keywords;

-- 2. Mevcut topic'leri sil (SADECE topics tablosunu, CASCADE YOK!)
DELETE FROM public.topics;

-- 3. Auto-increment'i sıfırla (1'den başlasın)
ALTER SEQUENCE public.topics_topic_id_seq RESTART WITH 1;

-- 4. Yeni topic verilerini ekle (topic_id otomatik atanacak)
INSERT INTO public.topics (name) VALUES
    ('Türkiye Gündemi'),        -- 1
    ('Dünya Gündemi'),          -- 2
    ('Ekonomi ve Finans'),      -- 3
    ('Futbol'),                 -- 4
    ('Basketbol'),              -- 5
    ('Voleybol'),               -- 6
    ('Motor Sporları'),         -- 7
    ('Yapay Zeka (AI)'),        -- 8
    ('Yazılım ve Donanım'),     -- 9
    ('Bilim ve Uzay'),          -- 10
    ('Oyun ve E-Spor'),         -- 11
    ('Girişimcilik ve Startup'),-- 12
    ('Sinema ve Dizi'),         -- 13
    ('Müzik'),                  -- 14
    ('Edebiyat ve Kitap'),      -- 15
    ('Sanat ve Tasarım'),       -- 16
    ('Sağlık ve İyi Yaşam'),    -- 17
    ('Seyahat ve Gezi'),        -- 18
    ('Otomotiv'),               -- 19
    ('Gastronomi');             -- 20

-- 5. Kontrol et
SELECT topic_id, name FROM public.topics ORDER BY topic_id;

