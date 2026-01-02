-- Summaries tablosuna topic_id kolonu ekle
-- Bu kolon AI'ın belirlediği konu bilgisini tutar

ALTER TABLE public.summaries 
ADD COLUMN IF NOT EXISTS topic_id INTEGER;

-- Foreign key constraint ekle
ALTER TABLE public.summaries 
ADD CONSTRAINT fk_summaries_topic 
FOREIGN KEY (topic_id) 
REFERENCES public.topics(topic_id)
ON DELETE SET NULL;

-- Index ekle (topic bazlı sorgular için performans)
CREATE INDEX IF NOT EXISTS idx_summaries_topic_id ON public.summaries(topic_id);

-- Yorum: Bu migration çalıştırıldıktan sonra, LLM Service haberleri özetlerken
-- aynı zamanda topic_id alanını da summary'e kaydedecek.

