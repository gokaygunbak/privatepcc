package com.pcc.llm_service.controller;

import com.pcc.llm_service.model.Topic;
import com.pcc.llm_service.service.GeminiService;
import com.pcc.llm_service.repository.SummaryRepository;
import com.pcc.llm_service.repository.TopicRepository;
import com.pcc.llm_service.model.Summary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/llm")
public class LlmController {

    private final GeminiService geminiService;
    private  final SummaryRepository summaryRepository;
    private final TopicRepository topicRepository;

    public LlmController(GeminiService geminiService, SummaryRepository summaryRepository, TopicRepository topicRepository) {
        this.geminiService = geminiService;
        this.summaryRepository = summaryRepository;
        this.topicRepository = topicRepository;
    }


    @GetMapping("/start-processing")
    public String startProcessing() {
        new Thread(() -> geminiService.processAllPendingContents()).start();
        return "Yapay Zeka işleme başladı! Konsolu takip et.";
    }

    // Özetlenmiş Haberleri Listele
    @GetMapping("/summaries")
    public List<Summary> getAllSummaries() {
        return summaryRepository.findAll();
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

    // İsteğe Bağlı Arama İçin: ID listesi verilen içerikleri özetle (yoksa) ve getir
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
}