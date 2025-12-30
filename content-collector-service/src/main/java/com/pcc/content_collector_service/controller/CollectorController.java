package com.pcc.content_collector_service.controller;

import com.pcc.content_collector_service.repository.ContentRepository;
import com.pcc.content_collector_service.service.RssCollectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/collector")
public class CollectorController {


    private final RssCollectionService collectionService;
    private final ContentRepository contentRepository;
    public CollectorController(RssCollectionService collectionService, ContentRepository contentRepository) {
        this.collectionService = collectionService;
        this.contentRepository = contentRepository;
    }

    //çalışıyor mu test metodu
    @GetMapping("/start")
    public String startCollection() {
        new Thread(() -> collectionService.collectAllContent()).start();

        return "İçerik toplama işlemi arka planda başlatıldı! Konsolu takip et.";
    }


    // filtreli rss verisi cekmek icin
    @GetMapping("/search")
    public java.util.List<com.pcc.content_collector_service.model.Content> searchContent(
            @org.springframework.web.bind.annotation.RequestParam String query) {
        // max 5
        try {
            System.out.println("Arama öncesi RSS verileri (Filtreli: " + query + ") güncelleniyor...");
            // Sadece bu kelimeyi içerenleri topla ve max 5 tane kaydet
            collectionService.collectAndSaveByKeyword(query, 5);
        } catch (Exception e) {
            System.err.println("RSS toplama hatası: " + e.getMessage());
        }

        java.util.List<com.pcc.content_collector_service.model.Content> results = contentRepository
                .findByOriginalTitleContainingIgnoreCaseOrOriginalTextContainingIgnoreCase(query, query);

        return results.stream().limit(5).collect(java.util.stream.Collectors.toList());
    }
}