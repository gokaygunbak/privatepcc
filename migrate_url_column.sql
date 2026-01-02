-- 🔧 URL Sütunu Migration
-- Google News URL'leri 255 karakteri aşıyor, bu yüzden sütun boyutunu artırıyoruz.
-- Bu SQL'i PostgreSQL'de çalıştırın.

-- contents tablosundaki original_url sütununu büyüt
ALTER TABLE public.contents 
ALTER COLUMN original_url TYPE VARCHAR(2000);

-- contents tablosundaki original_title sütununu büyüt
ALTER TABLE public.contents 
ALTER COLUMN original_title TYPE VARCHAR(500);

-- Kontrol için
SELECT column_name, data_type, character_maximum_length 
FROM information_schema.columns 
WHERE table_name = 'contents' 
AND column_name IN ('original_url', 'original_title');

