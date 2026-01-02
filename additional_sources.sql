-- 📰 EK RSS KAYNAKLARI
-- Bu SQL'i çalıştırarak daha fazla haber kaynağı ekleyebilirsiniz.
-- 
-- 📋 MEVCUT TOPICS TABLOSU:
-- topic_id = 1 → Teknoloji
-- topic_id = 2 → Futbol
-- topic_id = 3 → Spor
-- topic_id = 4 → Sanat & Kültür
-- topic_id = 5 → Ekonomi (YENİ)
-- topic_id = 6 → Dünya/Gündem (YENİ)
-- topic_id = 7 → Bilim & Sağlık (YENİ)

-- ═══════════════════════════════════════════════════════════════
-- 🏈 FUTBOL KAYNAKLARI (topic_id = 2) - Transfer, Süper Lig
-- ═══════════════════════════════════════════════════════════════
INSERT INTO public.sources ("name", url, topic_id, last_fetched_at) VALUES
    ('Fanatik', 'https://www.fanatik.com.tr/rss/futbol', 2, NULL),
    ('Fotomac', 'https://www.fotomac.com.tr/rss/anasayfa', 2, NULL),
    ('Goal Türkiye', 'https://www.goal.com/feeds/tr/news', 2, NULL),
    ('TRT Spor Futbol', 'https://www.trtspor.com.tr/rss/futbol.xml', 2, NULL),
    ('Transfermarkt TR', 'https://www.transfermarkt.com.tr/rss/news', 2, NULL)
ON CONFLICT DO NOTHING;

-- ═══════════════════════════════════════════════════════════════
-- ⚽ SPOR KAYNAKLARI (topic_id = 3) - Genel Spor
-- ═══════════════════════════════════════════════════════════════
INSERT INTO public.sources ("name", url, topic_id, last_fetched_at) VALUES
    ('NTV Spor', 'https://www.ntvspor.net/rss', 3, NULL),
    ('Sporx', 'https://www.sporx.com/rss/', 3, NULL),
    ('BeIN Sports TR', 'https://tr.beinsports.com/rss', 3, NULL),
    ('Milliyet Spor', 'https://www.milliyet.com.tr/rss/rssNew/sporRss.xml', 3, NULL),
    ('Hurriyet Spor', 'https://www.hurriyet.com.tr/rss/spor', 3, NULL),
    ('Sozcu Spor', 'https://www.sozcu.com.tr/rss/spor.xml', 3, NULL),
    ('TRT Spor', 'https://www.trtspor.com.tr/rss/spor.xml', 3, NULL),
    ('A Spor', 'https://www.aspor.com.tr/rss/', 3, NULL)
ON CONFLICT DO NOTHING;

-- ═══════════════════════════════════════════════════════════════
-- 💻 TEKNOLOJİ KAYNAKLARI (topic_id = 1)
-- ═══════════════════════════════════════════════════════════════
INSERT INTO public.sources ("name", url, topic_id, last_fetched_at) VALUES
    ('Webtekno', 'https://www.webtekno.com/rss.xml', 1, NULL),
    ('Donanim Haber', 'https://www.donanimhaber.com/rss/', 1, NULL),
    ('Chip Online', 'https://www.chip.com.tr/rss', 1, NULL),
    ('Log', 'https://www.log.com.tr/feed/', 1, NULL),
    ('Technopat', 'https://www.technopat.net/feed/', 1, NULL),
    ('Ars Technica', 'https://feeds.arstechnica.com/arstechnica/index', 1, NULL),
    ('The Verge', 'https://www.theverge.com/rss/index.xml', 1, NULL),
    ('TechCrunch', 'https://techcrunch.com/feed/', 1, NULL),
    ('Wired', 'https://www.wired.com/feed/rss', 1, NULL)
ON CONFLICT DO NOTHING;

-- ═══════════════════════════════════════════════════════════════
-- 🎭 SANAT & KÜLTÜR KAYNAKLARI (topic_id = 4)
-- ═══════════════════════════════════════════════════════════════
INSERT INTO public.sources ("name", url, topic_id, last_fetched_at) VALUES
    ('Hurriyet Kultur Sanat', 'https://www.hurriyet.com.tr/rss/kultur-sanat', 4, NULL),
    ('Milliyet Kultur', 'https://www.milliyet.com.tr/rss/rssNew/kulturRss.xml', 4, NULL),
    ('NTV Sanat', 'https://www.ntv.com.tr/sanat.rss', 4, NULL),
    ('Beyazperde', 'https://www.beyazperde.com/rss/', 4, NULL),
    ('FilmLoverss', 'https://www.filmloverss.com/feed/', 4, NULL)
ON CONFLICT DO NOTHING;

-- ═══════════════════════════════════════════════════════════════
-- 📈 EKONOMİ KAYNAKLARI (topic_id = 5) - YENİ TOPIC
-- ═══════════════════════════════════════════════════════════════
INSERT INTO public.topics ("name") VALUES
    ('Ekonomi')
ON CONFLICT DO NOTHING;

INSERT INTO public.sources ("name", url, topic_id, last_fetched_at) VALUES
    ('Bloomberg HT', 'https://www.bloomberght.com/rss', 5, NULL),
    ('Dunya Gazetesi', 'https://www.dunya.com/rss', 5, NULL),
    ('Para Analiz', 'https://www.paraanaliz.com/feed/', 5, NULL),
    ('Hurriyet Ekonomi', 'https://www.hurriyet.com.tr/rss/ekonomi', 5, NULL),
    ('NTV Ekonomi', 'https://www.ntv.com.tr/ekonomi.rss', 5, NULL)
ON CONFLICT DO NOTHING;

-- ═══════════════════════════════════════════════════════════════
-- 🌍 DÜNYA/GÜNDEM HABERLERİ (topic_id = 6) - YENİ TOPIC
-- ═══════════════════════════════════════════════════════════════
INSERT INTO public.topics ("name") VALUES
    ('Dünya & Gündem')
ON CONFLICT DO NOTHING;

INSERT INTO public.sources ("name", url, topic_id, last_fetched_at) VALUES
    ('CNN Turk', 'https://www.cnnturk.com/feed/rss/all/news', 6, NULL),
    ('Hurriyet Gundem', 'https://www.hurriyet.com.tr/rss/gundem', 6, NULL),
    ('Milliyet Gundem', 'https://www.milliyet.com.tr/rss/rssNew/gundemRss.xml', 6, NULL),
    ('Sozcu Gundem', 'https://www.sozcu.com.tr/rss/gundem.xml', 6, NULL),
    ('NTV Gundem', 'https://www.ntv.com.tr/gundem.rss', 6, NULL),
    ('DW Turkce', 'https://rss.dw.com/xml/rss-tur-all', 6, NULL),
    ('BBC Turkce', 'https://feeds.bbci.co.uk/turkce/rss.xml', 6, NULL)
ON CONFLICT DO NOTHING;

-- ═══════════════════════════════════════════════════════════════
-- 🔬 BİLİM & SAĞLIK (topic_id = 7) - YENİ TOPIC
-- ═══════════════════════════════════════════════════════════════
INSERT INTO public.topics ("name") VALUES
    ('Bilim & Sağlık')
ON CONFLICT DO NOTHING;

INSERT INTO public.sources ("name", url, topic_id, last_fetched_at) VALUES
    ('Bilim ve Teknik', 'https://bilimteknik.tubitak.gov.tr/rss', 7, NULL),
    ('Popular Science TR', 'https://www.popsci.com.tr/rss', 7, NULL),
    ('NTV Saglik', 'https://www.ntv.com.tr/saglik.rss', 7, NULL),
    ('Hurriyet Bilim', 'https://www.hurriyet.com.tr/rss/bilim', 7, NULL)
ON CONFLICT DO NOTHING;

-- ═══════════════════════════════════════════════════════════════
-- 🔍 GOOGLE NEWS DİNAMİK KAYNAKLAR (Popüler Aramalar)
-- Bu kaynaklar sabit arama terimleri için Google News RSS kullanır
-- ═══════════════════════════════════════════════════════════════
INSERT INTO public.sources ("name", url, topic_id, last_fetched_at) VALUES
    -- Futbol Takımları (topic_id = 2)
    ('Google - Galatasaray', 'https://news.google.com/rss/search?q=galatasaray&hl=tr&gl=TR&ceid=TR:tr', 2, NULL),
    ('Google - Fenerbahce', 'https://news.google.com/rss/search?q=fenerbahce&hl=tr&gl=TR&ceid=TR:tr', 2, NULL),
    ('Google - Besiktas', 'https://news.google.com/rss/search?q=besiktas&hl=tr&gl=TR&ceid=TR:tr', 2, NULL),
    ('Google - Trabzonspor', 'https://news.google.com/rss/search?q=trabzonspor&hl=tr&gl=TR&ceid=TR:tr', 2, NULL),
    ('Google - Super Lig', 'https://news.google.com/rss/search?q=super+lig+transfer&hl=tr&gl=TR&ceid=TR:tr', 2, NULL),
    
    -- Teknoloji Trendleri (topic_id = 1)
    ('Google - Yapay Zeka', 'https://news.google.com/rss/search?q=yapay+zeka&hl=tr&gl=TR&ceid=TR:tr', 1, NULL),
    ('Google - ChatGPT', 'https://news.google.com/rss/search?q=chatgpt&hl=tr&gl=TR&ceid=TR:tr', 1, NULL),
    ('Google - iPhone', 'https://news.google.com/rss/search?q=iphone&hl=tr&gl=TR&ceid=TR:tr', 1, NULL),
    
    -- Ekonomi Trendleri (topic_id = 5)
    ('Google - Dolar Kuru', 'https://news.google.com/rss/search?q=dolar+kuru&hl=tr&gl=TR&ceid=TR:tr', 5, NULL),
    ('Google - Borsa', 'https://news.google.com/rss/search?q=borsa+istanbul&hl=tr&gl=TR&ceid=TR:tr', 5, NULL)
ON CONFLICT DO NOTHING;
