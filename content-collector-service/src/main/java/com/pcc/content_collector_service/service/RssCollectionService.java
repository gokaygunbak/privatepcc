package com.pcc.content_collector_service.service;

import com.pcc.content_collector_service.model.Content;
import com.pcc.content_collector_service.model.Source;
import com.pcc.content_collector_service.repository.ContentRepository;
import com.pcc.content_collector_service.repository.SourceRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RssCollectionService {

    private SourceRepository sourceRepository;
    private ContentRepository contentRepository;

    public RssCollectionService(SourceRepository sourceRepository, ContentRepository contentRepository) {
        this.sourceRepository = sourceRepository;
        this.contentRepository = contentRepository;
    }

    // Tüm kaynakları gezip veri toplama emri
    public void collectAllContent() {
        List<Source> sources = sourceRepository.findAll();

        for (Source source : sources) {
            System.out.println("Taranıyor: " + source.getName());
            fetchFromSource(source);
        }
    }

    // Tek bir kaynaktan veri çekme (ROME kütüphanesi)
    private void fetchFromSource(Source source) {
        try {
            // URL bağlantısını aç
            java.net.HttpURLConnection httpcon = (java.net.HttpURLConnection) new URL(source.getUrl()).openConnection();

            // mozilla gibi tanıtma
            httpcon.addRequestProperty("User-Agent", "Mozilla/5.0");

            // httpcon rome kütüpe veriliyor
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(httpcon));

            for (SyndEntry entry : feed.getEntries()) {
                // exists ise atla
                if (contentRepository.existsByOriginalUrl(entry.getLink())) {
                    continue;
                }

                Content content = new Content();
                content.setSourceId(source.getSourceId());
                content.setOriginalTitle(entry.getTitle());
                content.setOriginalUrl(entry.getLink());

                if (entry.getDescription() != null) {
                    content.setOriginalText(entry.getDescription().getValue());
                } else {
                    content.setOriginalText("İçerik çekilemedi.");
                }

                // Tarih kontrolü (Bazen null gelebilir)
                if (entry.getPublishedDate() != null) {
                    content.setPublishedDate(entry.getPublishedDate().toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime());
                    // hata alırsa LocalDateTime.now()
                } else {
                    content.setPublishedDate(LocalDateTime.now());
                }

                content.setFetchStatus("PENDING");

                contentRepository.save(content);
                System.out.println("Kaydedildi: " + entry.getTitle());
            }

        } catch (Exception e) {
            System.err.println("Hata oluştu (" + source.getName() + "): " + e.getMessage());
        }
    }

    // Anahtar kelimeye göre filtreleyerek kaydetme
    public void collectAndSaveByKeyword(String keyword, int limit) {
        List<Source> sources = sourceRepository.findAll();
        int savedCount = 0;
        String lowerKeyword = keyword.toLowerCase(java.util.Locale.ENGLISH); // Basit küçük harf dönüşümü

        for (Source source : sources) {
            if (savedCount >= limit)
                break;

            try {
                java.net.HttpURLConnection httpcon = (java.net.HttpURLConnection) new URL(source.getUrl())
                        .openConnection();
                httpcon.addRequestProperty("User-Agent", "Mozilla/5.0");
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new XmlReader(httpcon));

                for (SyndEntry entry : feed.getEntries()) {
                    if (savedCount >= limit)
                        break;

                    String title = entry.getTitle() != null ? entry.getTitle() : "";
                    String desc = entry.getDescription() != null ? entry.getDescription().getValue() : "";

                    // Filtreleme Kontrolü
                    boolean matches = title.toLowerCase(java.util.Locale.ENGLISH).contains(lowerKeyword) ||
                            desc.toLowerCase(java.util.Locale.ENGLISH).contains(lowerKeyword);

                    if (matches) {
                        // Zaten var mı?
                        if (contentRepository.existsByOriginalUrl(entry.getLink())) {
                            continue;
                        }

                        Content content = new Content();
                        content.setSourceId(source.getSourceId());
                        content.setOriginalTitle(title);
                        content.setOriginalUrl(entry.getLink());
                        content.setOriginalText(desc.isEmpty() ? "İçerik çekilemedi." : desc);

                        if (entry.getPublishedDate() != null) {
                            content.setPublishedDate(entry.getPublishedDate().toInstant()
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDateTime());
                        } else {
                            content.setPublishedDate(LocalDateTime.now());
                        }

                        content.setFetchStatus("PENDING");
                        contentRepository.save(content);
                        System.out.println("Filtreli Kaydedildi: " + title);
                        savedCount++;
                    }
                }
            } catch (Exception e) {
                System.err.println("Filtreli Tarama Hatası (" + source.getName() + "): " + e.getMessage());
            }
        }
    }
}