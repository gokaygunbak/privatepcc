package com.pcc.llm_service.controller;

import com.pcc.llm_service.model.Topic;
import com.pcc.llm_service.model.Content;
import com.pcc.llm_service.service.GeminiService;
import com.pcc.llm_service.repository.SummaryRepository;
import com.pcc.llm_service.repository.ContentRepository;
import com.pcc.llm_service.repository.TopicRepository;
import com.pcc.llm_service.model.Summary;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.pcc.llm_service.dto.TrendsRequest;
import com.pcc.llm_service.dto.TrendsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/llm")
public class LlmController {

    private final GeminiService geminiService;
    private final SummaryRepository summaryRepository;
    private final ContentRepository contentRepository;
    private final TopicRepository topicRepository;

    public LlmController(GeminiService geminiService, SummaryRepository summaryRepository,
            ContentRepository contentRepository, TopicRepository topicRepository) {
        this.geminiService = geminiService;
        this.summaryRepository = summaryRepository;
        this.contentRepository = contentRepository;
        this.topicRepository = topicRepository;
    }

    @GetMapping("/start-processing")
    public String startProcessing() {
        new Thread(() -> geminiService.processAllPendingContents()).start();
        return "Yapay Zeka işleme başladı! Konsolu takip et.";
    }

    // Özetlenmiş Haberleri Listele (eski - tüm verileri çeker)
    @GetMapping("/summaries")
    public List<Summary> getAllSummaries() {
        return summaryRepository.findAll();
    }

    // Özetlenmiş Haberleri Sayfalı Listele (published_date'e göre sıralı)
    @GetMapping("/summaries/paged")
    public ResponseEntity<Page<Summary>> getPagedSummaries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Summary> summaryPage = summaryRepository.findAllByOrderByPublishedDateDesc(pageable);
        return ResponseEntity.ok(summaryPage);
    }

    // Frontend'de kullanıcıya "Hangi konuları seversin?" diye sormak için
    // konuları listele
    @GetMapping("/topics")
    public ResponseEntity<List<Topic>> getAllTopics() {
        // TopicRepository'nin otomatik oluşturduğu findAll() metodunu çağırıyoruz
        return ResponseEntity.ok(topicRepository.findAll());
    }

    // Interaction Service in konuya göre haberleri istediği yer
    @GetMapping("/summaries/by-topics")
    public ResponseEntity<List<Summary>> getSummariesByTopics(@RequestParam List<Integer> topicIds) {
        // Repository'e yeni eklediğimiz sorguyu çağırıyoruz
        return ResponseEntity.ok(summaryRepository.findByTopicIdIn(topicIds));
    }

    // ContentId'den Topic ID'yi getir (Interaction Service için)
    @GetMapping("/summaries/topic-by-content/{contentId}")
    public ResponseEntity<Integer> getTopicIdByContentId(@PathVariable java.util.UUID contentId) {
        Summary summary = summaryRepository.findByContentId(contentId);
        if (summary != null && summary.getTopic() != null) {
            return ResponseEntity.ok(summary.getTopic().getTopicId());
        }
        return ResponseEntity.ok(null); // Topic atanmamış
    }

    // ContentId listesine göre summary'leri getir (Kaydedilen içerikler için)
    @GetMapping("/summaries/by-contents")
    public ResponseEntity<List<Summary>> getSummariesByContentIds(@RequestParam List<java.util.UUID> contentIds) {
        if (contentIds == null || contentIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<Summary> summaries = summaryRepository.findAll().stream()
                .filter(s -> s.getContent() != null && contentIds.contains(s.getContent().getContentId()))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(summaries);
    }

    // İsteğe Bağlı Arama İçin: ID listesi verilen içerikleri özetle (yoksa) ve
    // getir
    @PostMapping("/summarize-batch")
    public ResponseEntity<List<Summary>> summarizeBatch(@RequestBody List<java.util.UUID> contentIds) {
        // Önce bu içeriklerin özetleri zaten var mı kontrol et, olmayanları oluştur
        contentIds.forEach(id -> {
            // "existsByContentId" -> "existsByContent_ContentId"
            if (!summaryRepository.existsByContent_ContentId(id)) {
                try {
                    geminiService.processContentById(id);
                } catch (Exception e) {
                    System.err.println("Özetleme hatası ID: " + id + " -> " + e.getMessage());
                }
            }
        });

        // Şimdi özetleri getir
        List<Summary> summaries = summaryRepository.findAll().stream()
                // "s.getContentId()" -> "s.getContent().getContentId()"
                .filter(s -> s.getContent() != null && contentIds.contains(s.getContent().getContentId()))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(summaries);
    }

    // Admin: İçeriği ve Summary'yi Sil
    @DeleteMapping("/content/{contentId}")
    @Transactional
    public ResponseEntity<String> deleteContent(@PathVariable java.util.UUID contentId) {
        try {
            System.out.println("🗑️ LLM Service: İçerik siliniyor -> " + contentId);

            // 1. Önce bu içeriğe ait summary'yi sil
            Summary summary = summaryRepository.findByContentId(contentId);
            if (summary != null) {
                summaryRepository.delete(summary);
                System.out.println("   ✓ Summary silindi");
            }

            // 2. Sonra content'i sil
            Content content = contentRepository.findById(contentId).orElse(null);
            if (content != null) {
                contentRepository.delete(content);
                System.out.println("   ✓ Content silindi");
            }

            return ResponseEntity.ok("İçerik başarıyla silindi.");
        } catch (Exception e) {
            System.err.println("❌ Silme hatası: " + e.getMessage());
            return ResponseEntity.badRequest().body("Silme hatası: " + e.getMessage());
        }
    }

    // Yeni Endpoint: Kullanıcıya özel rastgele ve görülmemiş içerik getir
    // (Opsiyonel Topic ID ile)
    @GetMapping("/feed/random-unseen")
    public ResponseEntity<Summary> getRandomUnseenContent(@RequestParam Long userId,
            @RequestParam(required = false) Integer topicId) {
        java.util.Optional<Summary> summary = geminiService.getAndLogRandomUnseenContent(userId, topicId);
        return summary.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/stats/summary-count")
    public ResponseEntity<Long> getSummaryCount() {
        return ResponseEntity.ok(summaryRepository.count());
    }

    // Admin: Bekleyen (PENDING) içerik sayısını getir
    @GetMapping("/stats/pending-count")
    public ResponseEntity<Long> getPendingContentCount() {
        return ResponseEntity.ok(contentRepository.countByFetchStatus("PENDING"));
    }

    // Admin: Kategorilere göre içerik sayılarını getir
    @GetMapping("/stats/topics")
    public ResponseEntity<List<com.pcc.llm_service.dto.TopicStatsDto>> getTopicStats() {
        return ResponseEntity.ok(summaryRepository.countSummariesByTopic());
    }

    // --- Trends Page Endpoint ---
    @PostMapping("/feed/trends")
    public ResponseEntity<TrendsResponse> getTrendsFeed(@RequestBody TrendsRequest request) {
        Long userId = request.getUserId();
        int size = request.getSize();
        if (size <= 0)
            size = 10;

        // 1. Toplam görülmemiş içerik sayısını hesapla
        long totalElements = summaryRepository.countUnseenSummaries(userId);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        List<Summary> summaries;
        // 2. İçerikleri çek
        if (request.getExcludeIds() == null || request.getExcludeIds().isEmpty()) {
            summaries = summaryRepository.findRandomUnseenSummaries(userId, size);
        } else {
            summaries = summaryRepository.findRandomUnseenSummariesWithExclusion(userId, request.getExcludeIds(), size);
        }

        return ResponseEntity.ok(new TrendsResponse(summaries, totalPages, totalElements));
    }
}