package com.pcc.interaction_service.controller;

import com.pcc.interaction_service.dto.InteractionRequest;
import com.pcc.interaction_service.dto.SummaryDto;
import com.pcc.interaction_service.dto.TopicDto;
import com.pcc.interaction_service.dto.PreferenceRequest;
import com.pcc.interaction_service.client.LlmServiceClient;
import com.pcc.interaction_service.entity.UserInteraction;
import com.pcc.interaction_service.repository.UserInteractionRepository;
import com.pcc.interaction_service.service.UserPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pcc.interaction_service.dto.TopicScoreDto;
import com.pcc.interaction_service.repository.UserTopicScoreRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/interactions")
public class InteractionController {

    private final UserPreferenceService preferenceService;
    private final LlmServiceClient llmServiceClient;
    private final UserInteractionRepository interactionRepository;
    private final UserTopicScoreRepository userTopicScoreRepository;

    public InteractionController(UserPreferenceService preferenceService,
            LlmServiceClient llmServiceClient,
            UserInteractionRepository interactionRepository,
            UserTopicScoreRepository userTopicScoreRepository) {
        this.preferenceService = preferenceService;
        this.llmServiceClient = llmServiceClient;
        this.interactionRepository = interactionRepository;
        this.userTopicScoreRepository = userTopicScoreRepository;
    }

    // Tüm Konuları Listele (Kullanıcı seçim yapsın diye)
    // Bunu direkt LLM servinden de isteyebiliriz ama buradan geçirmek daha temiz
    // (Gateway tek kapı).
    @GetMapping("/topics")
    public ResponseEntity<List<TopicDto>> getAllTopics() {
        return ResponseEntity.ok(llmServiceClient.getAllTopics());
    }

    // Kullanıcının İlgi Alanlarını Kaydet (Onboarding)
    @PostMapping("/preferences")
    public ResponseEntity<String> savePreferences(@RequestBody PreferenceRequest request) {
        System.out.println("Gelen Tercih İsteği: UserID=" + request.getUserId() + ", Topics=" + request.getTopicIds());
        try {
            preferenceService.saveUserPreferences(request.getUserId(), request.getTopicIds());
            return ResponseEntity.ok("Tercihler başarıyla kaydedildi! 🚀");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("HATA DETAYI: " + e.getMessage());
        }
    }

    // Etkileşim Kaydet (Like/View/Save)
    @PostMapping("/interact")
    public ResponseEntity<String> recordInteraction(@RequestBody InteractionRequest request) {
        System.out.println("GELEN INTERACTION: User=" + request.getUserId() + ", Type=" + request.getInteractionType()
                + ", TopicID=" + request.getTopicId());
        preferenceService.recordInteraction(request);
        return ResponseEntity.ok("Etkileşim kaydedildi.");
    }

    // Kişiselleştirilmiş Akışı Getir
    @GetMapping("/feed")
    public ResponseEntity<List<SummaryDto>> getPersonalizedFeed(@RequestParam Long userId) {
        return ResponseEntity.ok(preferenceService.getPersonalizedFeed(userId));
    }

    // Rastgele ve daha önce görülmemiş bir sonraki içeriği getir
    @GetMapping("/feed/next-random")
    public ResponseEntity<SummaryDto> getNextRandomContent(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "false") boolean forceTop) {

        SummaryDto summary = preferenceService.getNextWeightedContent(userId, forceTop);

        if (summary != null) {
            return ResponseEntity.ok(summary);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    // Kullanıcının Seçtiği İlgi Alanlarını Getir (Profil sayfası için)
    @GetMapping("/preferences/{userId}")
    public ResponseEntity<List<TopicDto>> getUserPreferences(@PathVariable Long userId) {
        List<TopicDto> userTopics = preferenceService.getUserSelectedTopics(userId);
        return ResponseEntity.ok(userTopics);
    }

    // Kullanıcının Kaydettiği İçerikleri Getir
    @GetMapping("/saved/{userId}")
    public ResponseEntity<List<SummaryDto>> getSavedContents(@PathVariable Long userId) {
        List<SummaryDto> savedContents = preferenceService.getSavedContents(userId);
        return ResponseEntity.ok(savedContents);
    }

    // Admin: Şikayet Edilen İçerikleri Getir
    @GetMapping("/reports")
    public ResponseEntity<List<SummaryDto>> getReportedContents() {
        List<SummaryDto> reportedContents = preferenceService.getReportedContents();
        return ResponseEntity.ok(reportedContents);
    }

    // Admin: İçeriği ve İlişkili Tüm Verileri Sil
    @DeleteMapping("/content/{contentId}")
    public ResponseEntity<String> deleteContent(@PathVariable java.util.UUID contentId) {
        try {
            preferenceService.deleteContentCompletely(contentId);
            return ResponseEntity.ok("İçerik başarıyla silindi.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Silme hatası: " + e.getMessage());
        }
    }

    // Admin: Şikayeti Yoksay (Dismiss Report)
    @DeleteMapping("/reports/{contentId}")
    public ResponseEntity<String> dismissReport(@PathVariable java.util.UUID contentId) {
        try {
            preferenceService.dismissReport(contentId);
            return ResponseEntity.ok("Şikayet yoksayıldı (silindi).");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("İşlem hatası: " + e.getMessage());
        }
    }

    // Admin: Toplam etkileşim sayısını getir (REPORT hariç)
    @GetMapping("/stats/interaction-count")
    public ResponseEntity<Long> getInteractionCount() {
        // REPORT tipindeki etkileşimleri saymıyoruz
        long count = interactionRepository.countByInteractionTypeNot(UserInteraction.InteractionType.REPORT);
        return ResponseEntity.ok(count);
    }

    // İstatistikler: Topic Skorlarını Getir
    @GetMapping("/stats/topic-scores")
    public ResponseEntity<List<com.pcc.interaction_service.dto.TopicScoreDto>> getTopicScoreStats(
            @RequestParam Long userId) {
        return ResponseEntity.ok(preferenceService.getUserTopicStats(userId));
    }

    // Kullanıcının Algoritmasını Sıfırla (Danger Zone)
    @DeleteMapping("/reset")
    @Transactional
    public ResponseEntity<String> resetUserAlgorithm(@RequestParam Long userId) {
        userTopicScoreRepository.deleteByUserId(userId);
        interactionRepository.deleteByUserId(userId); // Changed from userInteractionRepository to interactionRepository
        return ResponseEntity.ok("Algoritma sıfırlandı.");
    }

    @GetMapping("/stats/popular-topics")
    public ResponseEntity<List<TopicScoreDto>> getPopularTopics() {
        // 1. En yüksek skorlu 5 topic'i çek (ID ve Total Score)
        List<Object[]> results = userTopicScoreRepository.findTopTopicsByTotalScore(
                PageRequest.of(0, 5));

        if (results.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        // 2. Topic İsimlerini LLM Service'den çek
        List<com.pcc.interaction_service.dto.TopicDto> allTopics = llmServiceClient.getAllTopics();
        Map<Integer, String> topicNameMap = allTopics.stream()
                .collect(Collectors.toMap(
                        com.pcc.interaction_service.dto.TopicDto::getTopicId,
                        com.pcc.interaction_service.dto.TopicDto::getName));

        // 3. DTO'ları oluştur
        List<TopicScoreDto> dtos = results.stream().map(row -> {
            Integer topicId = (Integer) row[0];
            Double totalScore = (Double) row[1];
            String topicName = topicNameMap.getOrDefault(topicId, "Bilinmeyen Kategori");

            // Percentage şimdilik 0
            return new TopicScoreDto(topicId, topicName, totalScore, 0.0);
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}