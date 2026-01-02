package com.pcc.interaction_service.controller;

import com.pcc.interaction_service.dto.InteractionRequest;
import com.pcc.interaction_service.dto.SummaryDto;
import com.pcc.interaction_service.dto.TopicDto;
import com.pcc.interaction_service.dto.PreferenceRequest;
import com.pcc.interaction_service.client.LlmServiceClient;
import com.pcc.interaction_service.service.UserPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interactions")
// @RequiredArgsConstructor
public class InteractionController {

    private final UserPreferenceService preferenceService;
    private final LlmServiceClient llmServiceClient;

    public InteractionController(UserPreferenceService preferenceService,
            LlmServiceClient llmServiceClient) {
        this.preferenceService = preferenceService;
        this.llmServiceClient = llmServiceClient;
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

    // Kullanıcının Seçtiği İlgi Alanlarını Getir (Profil sayfası için)
    @GetMapping("/preferences/{userId}")
    public ResponseEntity<List<TopicDto>> getUserPreferences(@PathVariable Long userId) {
        List<TopicDto> userTopics = preferenceService.getUserSelectedTopics(userId);
        return ResponseEntity.ok(userTopics);
    }

}