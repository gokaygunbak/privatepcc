package com.pcc.interaction_service.service;

import com.pcc.interaction_service.client.LlmServiceClient;
import com.pcc.interaction_service.dto.InteractionRequest;
import com.pcc.interaction_service.dto.SummaryDto;
import com.pcc.interaction_service.entity.UserInteraction;
import com.pcc.interaction_service.entity.UserTopicPreference;
import com.pcc.interaction_service.entity.UserTopicScore;
import com.pcc.interaction_service.repository.UserInteractionRepository;
import com.pcc.interaction_service.repository.UserTopicPreferenceRepository;
import com.pcc.interaction_service.repository.UserTopicScoreRepository;
import com.pcc.interaction_service.entity.UserTopicScoreId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
// @RequiredArgsConstructor // Constructor Injection (Autowired yerine geçer,
// daha temizdir)
public class UserPreferenceService {

    private final UserTopicPreferenceRepository preferenceRepository;
    private final UserTopicScoreRepository scoreRepository;
    private final UserInteractionRepository interactionRepository;
    private final LlmServiceClient llmServiceClient; // Feign Client (Telefon Hattı)

    public UserPreferenceService(UserTopicPreferenceRepository preferenceRepository,
            UserTopicScoreRepository scoreRepository,
            UserInteractionRepository interactionRepository,
            LlmServiceClient llmServiceClient) {
        this.preferenceRepository = preferenceRepository;
        this.scoreRepository = scoreRepository;
        this.interactionRepository = interactionRepository;
        this.llmServiceClient = llmServiceClient;
    }

    // 1. Kullanıcının ilgi alanlarını kaydet (Onboarding)
    @Transactional
    public void saveUserPreferences(Long userId, List<Integer> topicIds) {
        // A. Eskileri temizle (Onboarding tekrar yapılırsa)
        List<UserTopicPreference> existing = preferenceRepository.findByUserId(userId);
        preferenceRepository.deleteAll(existing);

        // B. Yeni seçimleri ve Başlangıç Skorlarını kaydet
        for (Integer topicId : topicIds) {
            // Tercihi kaydet
            UserTopicPreference pref = new UserTopicPreference();
            pref.setUserId(userId);
            pref.setTopicId(topicId);
            preferenceRepository.save(pref);

            // Başlangıç Skorunu Ata (Örn: 5.0 ile başlasın)
            updateUserTopicScore(userId, topicId, 5.0);
        }
    }

    // 2. Etkileşimi Kaydet ve Puanla
    @Transactional
    public void recordInteraction(InteractionRequest request) {
        // A. Etkileşimi Veritabanına Yaz (Loglama)
        UserInteraction interaction = new UserInteraction();
        interaction.setUserId(request.getUserId());
        interaction.setContentId(request.getContentId());
        interaction.setInteractionType(request.getInteractionType());
        interactionRepository.save(interaction);

        // B. Konu Puanını Güncelle (Eğer konu bilgisi varsa)
        if (request.getTopicId() != null) {
            double scoreIncrement = getScoreByInteractionType(request.getInteractionType());
            updateUserTopicScore(request.getUserId(), request.getTopicId(), scoreIncrement);
        }
    }

    // Yardımcı Metod: Puan Güncelleme
    private void updateUserTopicScore(Long userId, Integer topicId, double scoreDelta) {
        UserTopicScoreId id = new UserTopicScoreId();
        id.setUserId(userId);
        id.setTopicId(topicId);

        Optional<UserTopicScore> existingScore = scoreRepository.findById(id);
        UserTopicScore scoreEntity;
        double currentScore;
        double newScore;

        if (existingScore.isPresent()) {
            scoreEntity = existingScore.get();
            currentScore = scoreEntity.getScore();
            newScore = currentScore + scoreDelta;
            scoreEntity.setScore(newScore); // Update logic inside
        } else {
            scoreEntity = new UserTopicScore();
            scoreEntity.setUserId(userId);
            scoreEntity.setTopicId(topicId);
            currentScore = 0.0;
            newScore = scoreDelta;
            scoreEntity.setScore(newScore);
        }

        scoreEntity.setLastUpdated(LocalDateTime.now());
        scoreRepository.save(scoreEntity);
        System.out.println("SKOR GÜNCELLENDİ: User=" + userId + ", Topic=" + topicId + ", Eski=" + currentScore
                + ", Yeni=" + newScore + " 📈");
    }

    // Hangi interaction kaç puan
    private double getScoreByInteractionType(UserInteraction.InteractionType type) {
        if (type == null)
            return 0.0;
        return switch (type) {
            case LIKE -> 1.0;
            case SAVE -> 2.0;
            case VIEW -> 0.1;
            default -> 0.0;
        };
    }

    // 3. Kişiselleştirilmiş Akışı Getir
    public List<SummaryDto> getPersonalizedFeed(Long userId) {
        // A. Kullanıcının sevdiği konuların ID'lerini çek
        // TODO: Burayı artık "En yüksek puanlı konular" olarak değiştirebiliriz.
        // Şimdilik eski mantık (seçilenler) kalsın veya ikisini birleştirebiliriz.
        List<Integer> topicIds = preferenceRepository.findTopicIdsByUserId(userId);

        // B. Eğer hiç tercihi yoksa boş liste veya genel akış dönülebilir
        if (topicIds.isEmpty()) {
            return List.of();
        }

        // C. LLM Servisini ara ve bu ID'lere ait haberleri iste!
        return llmServiceClient.getSummariesByTopics(topicIds);
    }
}