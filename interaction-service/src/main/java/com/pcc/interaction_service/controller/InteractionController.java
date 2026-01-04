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

import java.util.List;

@RestController
@RequestMapping("/api/interactions")
public class InteractionController {

    private final UserPreferenceService preferenceService;
    private final LlmServiceClient llmServiceClient;
    private final UserInteractionRepository interactionRepository;

    public InteractionController(UserPreferenceService preferenceService,
            LlmServiceClient llmServiceClient,
            UserInteractionRepository interactionRepository) {
        this.preferenceService = preferenceService;
        this.llmServiceClient = llmServiceClient;
        this.interactionRepository = interactionRepository;
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

    // Rastgele ve daha önce görülmemiş bir sonraki içeriği getir (Sonsuz Kaydırma
    // için)
    // Rastgele ve daha önce görülmemiş bir sonraki içeriği getir (Sonsuz Kaydırma
    // için)
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
    @PostMapping("/reset")
    public ResponseEntity<String> resetAlgorithm(@RequestParam Long userId) {
        try {
            preferenceService.resetUserAlgorithm(userId);
            return ResponseEntity.ok("Algoritma sıfırlandı.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Sıfırlama hatası: " + e.getMessage());
        }
    }
}