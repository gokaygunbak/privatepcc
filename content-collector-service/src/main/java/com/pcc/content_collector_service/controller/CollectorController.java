package com.pcc.content_collector_service.controller;

import com.pcc.content_collector_service.model.Content;
import com.pcc.content_collector_service.repository.ContentRepository;
import com.pcc.content_collector_service.service.RssCollectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/collector")
public class CollectorController {

    private final RssCollectionService collectionService;
    private final ContentRepository contentRepository;

    public CollectorController(RssCollectionService collectionService, ContentRepository contentRepository) {
        this.collectionService = collectionService;
        this.contentRepository = contentRepository;
    }

    @GetMapping("/start")
    public String startCollection() {
        new Thread(() -> collectionService.collectAllContent()).start();
        return " İçerik toplama işlemi arka planda başlatıldı! Konsolu takip et.";
    }

    /*
     * 
     * 1. Google News'ten dinamik arama yapar
     * 2. Kayıtlı RSS kaynaklarından arama yapar
     * 3. Veritabanındaki mevcut içeriklerden arama yapar
     * 4. Sonuçları birleştirir ve tekrarları kaldırır
     */
    @GetMapping("/search")
    public List<Content> searchContent(@RequestParam String query) {
        System.out.println("ARAMA İSTEĞİ: \"" + query + "\"");

        Set<UUID> seenIds = new HashSet<>();
        List<Content> finalResults = new ArrayList<>();
        int maxResults = 5;

        // Önce Google News + kayıtlı kaynaklardan yeni içerik topla
        try {
            List<Content> freshContents = collectionService.collectAndSaveByKeyword(query, maxResults);
            for (Content c : freshContents) {
                if (c.getContentId() != null && seenIds.add(c.getContentId())) {
                    finalResults.add(c);
                }
            }
            System.out.println("Yeni toplanan içerik sayısı: " + freshContents.size());
        } catch (Exception e) {
            System.err.println(" İçerik toplama hatası: " + e.getMessage());
        }

        // Veritabanındaki mevcut içeriklerden de ara
        try {
            List<Content> dbResults = contentRepository
                    .findByOriginalTitleContainingIgnoreCaseOrOriginalTextContainingIgnoreCase(query, query);
            
            for (Content c : dbResults) {
                if (finalResults.size() >= maxResults) break;
                if (c.getContentId() != null && seenIds.add(c.getContentId())) {
                    finalResults.add(c);
                }
            }
            System.out.println("Veritabanından bulunan ek içerik: " + dbResults.size());
        } catch (Exception e) {
            System.err.println(" Veritabanı arama hatası: " + e.getMessage());
        }

        //Sonuçları tarihe göre sırala (en yeni önce)
        finalResults.sort((a, b) -> {
            if (a.getPublishedDate() == null) return 1;
            if (b.getPublishedDate() == null) return -1;
            return b.getPublishedDate().compareTo(a.getPublishedDate());
        });

        //Maksimum sonuç sayısını uygula
        List<Content> limitedResults = finalResults.stream()
                .limit(maxResults)
                .collect(Collectors.toList());
        System.out.println("TOPLAM SONUÇ: " + limitedResults.size() + " haber");

        return limitedResults;
    }

    @GetMapping("/search/google")
    public List<Content> searchFromGoogle(@RequestParam String query) {
        return collectionService.searchFromGoogleNews(query, 5);
    }

    @GetMapping("/stats")
    public String getStats() {
        long totalContents = contentRepository.count();
        return String.format("""
            📊 Content Collector İstatistikleri
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            📰 Toplam İçerik: %d
            🔗 Kaynak Sayısı: (sources tablosundan)
            ⏱️ Son Güncelleme: Şimdi
            """, totalContents);
    }
}