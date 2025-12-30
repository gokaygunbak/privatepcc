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

    // 1. Haberleri Özetle (Tetikleyici)
    @GetMapping("/start-processing")
    public String startProcessing() {
        new Thread(() -> geminiService.processAllPendingContents()).start();
        return "Yapay Zeka işleme başladı! Konsolu takip et.";
    }

    // 2. Özetlenmiş Haberleri Listele (React Buraya İstek Atıyor)
    @GetMapping("/summaries")
    public List<Summary> getAllSummaries() {
        return summaryRepository.findAll();
    }

    // LlmController.java'nın içine eklenecekler:

    // 1. Frontend'de kullanıcıya "Hangi konuları seversin?" diye sormak için
    // konuları listele
    @GetMapping("/topics")
    public ResponseEntity<List<Topic>> getAllTopics() {
        // TopicRepository'nin otomatik oluşturduğu findAll() metodunu çağırıyoruz
        return ResponseEntity.ok(topicRepository.findAll());
    }

    // 2. Interaction Service'in arayıp "Bana şu konulardaki haberleri ver" dediği
    // yer
    @GetMapping("/summaries/by-topics")
    public ResponseEntity<List<Summary>> getSummariesByTopics(@RequestParam List<Integer> topicIds) {
        // Repository'e yeni eklediğimiz sorguyu çağırıyoruz
        return ResponseEntity.ok(summaryRepository.findByTopicIdIn(topicIds));
    }

    // 3. İsteğe Bağlı Arama İçin: ID listesi verilen içerikleri özetle (yoksa) ve
    // getir
    @PostMapping("/summarize-batch")
    public ResponseEntity<List<Summary>> summarizeBatch(@RequestBody List<java.util.UUID> contentIds) {
        // 1. Önce bu içeriklerin özetleri zaten var mı kontrol et, olmayanları oluştur
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

        // 2. Şimdi özetleri getir
        List<Summary> summaries = summaryRepository.findAll().stream()
                // "s.getContentId()" -> "s.getContent().getContentId()"
                .filter(s -> s.getContent() != null && contentIds.contains(s.getContent().getContentId()))
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(summaries);
    }
}