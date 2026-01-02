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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
// @RequiredArgsConstructor
public class UserPreferenceService {

    private final UserTopicPreferenceRepository preferenceRepository;
    private final UserTopicScoreRepository scoreRepository;
    private final UserInteractionRepository interactionRepository;
    private final LlmServiceClient llmServiceClient; // Feign Client

    public UserPreferenceService(UserTopicPreferenceRepository preferenceRepository,
            UserTopicScoreRepository scoreRepository,
            UserInteractionRepository interactionRepository,
            LlmServiceClient llmServiceClient) {
        this.preferenceRepository = preferenceRepository;
        this.scoreRepository = scoreRepository;
        this.interactionRepository = interactionRepository;
        this.llmServiceClient = llmServiceClient;
    }

    // Kullanıcının ilgi alanlarını kaydet (Onboarding)
    @Transactional
    public void saveUserPreferences(Long userId, List<Integer> newTopicIds) {
        // 1. Mevcut seçili topic'leri al
        List<Integer> oldTopicIds = preferenceRepository.findTopicIdsByUserId(userId);
        
        // Debug: Mevcut skorları da kontrol et
        List<UserTopicScore> existingScores = scoreRepository.findByUserIdOrderByScoreDesc(userId);
        System.out.println("🔍 DEBUG - Mevcut skorlar: " + existingScores.stream()
            .map(s -> "Topic=" + s.getTopicId() + ",Skor=" + s.getScore())
            .collect(java.util.stream.Collectors.joining(", ")));
        
        Set<Integer> oldSet = new HashSet<>(oldTopicIds);
        Set<Integer> newSet = new HashSet<>(newTopicIds);

        // 2. Kaldırılan topic'leri bul (eski - yeni)
        Set<Integer> removedTopics = new HashSet<>(oldSet);
        removedTopics.removeAll(newSet);

        // 3. Yeni eklenen topic'leri bul (yeni - eski)
        Set<Integer> addedTopics = new HashSet<>(newSet);
        addedTopics.removeAll(oldSet);

        // 4. Korunan topic'leri bul (kesişim) - bunlara dokunmayacağız
        Set<Integer> keptTopics = new HashSet<>(oldSet);
        keptTopics.retainAll(newSet);

        System.out.println("📊 Tercih Değişikliği - User=" + userId);
        System.out.println("   Eski: " + oldTopicIds);
        System.out.println("   Yeni: " + newTopicIds);
        System.out.println("   ➕ Eklenen: " + addedTopics);
        System.out.println("   ➖ Kaldırılan: " + removedTopics);
        System.out.println("   ✓ Korunan: " + keptTopics);

        // 5. Tercihleri güncelle (hepsini sil, yeniden ekle)
        preferenceRepository.deleteAllByUserId(userId);
        preferenceRepository.flush();

        for (Integer topicId : newTopicIds) {
            UserTopicPreference pref = new UserTopicPreference();
            pref.setUserId(userId);
            pref.setTopicId(topicId);
            preferenceRepository.save(pref);
        }

        // 6. Kaldırılan topic'lerin skorlarını sil
        for (Integer topicId : removedTopics) {
            UserTopicScoreId scoreId = new UserTopicScoreId(userId, topicId);
            scoreRepository.deleteById(scoreId);
            System.out.println("🗑️ Skor silindi: Topic=" + topicId);
        }

        // 7. Yeni eklenen topic'lere başlangıç puanı ver (SADECE skor yoksa!)
        for (Integer topicId : addedTopics) {
            UserTopicScoreId scoreId = new UserTopicScoreId(userId, topicId);
            
            // Eğer bu topic için zaten skor varsa, DOKUNMA!
            if (scoreRepository.existsById(scoreId)) {
                System.out.println("⏭️ Topic=" + topicId + " için skor zaten var, atlanıyor.");
                continue;
            }
            
            UserTopicScore newScore = new UserTopicScore(userId, topicId, 5.0);
            scoreRepository.save(newScore);
            System.out.println("✨ Yeni skor oluşturuldu: Topic=" + topicId + ", Skor=5.0");
        }

        // 8. Korunan topic'lerin skorlarına DOKUNMA (mevcut skorları koru)
        System.out.println("✅ İşlem tamamlandı. Korunan topic'lerin skorları değişmedi.");
    }

    // Etkileşimi Kaydet ve Puanla
    @Transactional
    public void recordInteraction(InteractionRequest request) {
        // Etkileşimi Veritabanına Yaz (Loglama)
        UserInteraction interaction = new UserInteraction();
        interaction.setUserId(request.getUserId());
        interaction.setContentId(request.getContentId());
        interaction.setInteractionType(request.getInteractionType());
        interactionRepository.save(interaction);

        // Topic ID'yi belirle: Önce request'ten, yoksa LLM Service'den çek
        Integer topicId = request.getTopicId();
        
        if (topicId == null && request.getContentId() != null) {
            try {
                // ContentId'den Summary'nin topic_id'sini çek
                topicId = llmServiceClient.getTopicIdByContentId(request.getContentId());
                System.out.println("🎯 Topic ID LLM Service'den alındı: " + topicId + " (ContentId: " + request.getContentId() + ")");
            } catch (Exception e) {
                System.err.println("⚠️ Topic ID alınamadı: " + e.getMessage());
            }
        }

        // Konu Puanını Güncelle (Eğer konu bilgisi varsa)
        if (topicId != null) {
            double scoreIncrement = getScoreByInteractionType(request.getInteractionType());
            updateUserTopicScore(request.getUserId(), topicId, scoreIncrement);
        } else {
            System.out.println("⚠️ Topic ID bulunamadı, puanlama yapılmadı.");
        }
    }

    // Puan Güncelleme
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
                + ", Yeni=" + newScore);
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

    // Kişiselleştirilmiş Akışı Getir
    public List<SummaryDto> getPersonalizedFeed(Long userId) {
        //  Kullanıcının sevdiği konuların ID'lerini çek
        List<Integer> topicIds = preferenceRepository.findTopicIdsByUserId(userId);

        // Eğer hiç tercihi yoksa boş liste
        if (topicIds.isEmpty()) {
            return List.of();
        }

        // LLM Servisini ara ve bu ID'lere ait haberleri iste
        return llmServiceClient.getSummariesByTopics(topicIds);
    }

    // Kullanıcının Seçtiği İlgi Alanlarını Getir (Profil sayfası için)
    public List<com.pcc.interaction_service.dto.TopicDto> getUserSelectedTopics(Long userId) {
        // 1. Kullanıcının seçtiği topic ID'lerini al
        List<Integer> userTopicIds = preferenceRepository.findTopicIdsByUserId(userId);
        
        if (userTopicIds.isEmpty()) {
            return List.of();
        }

        // 2. Tüm konuları LLM Service'den al
        List<com.pcc.interaction_service.dto.TopicDto> allTopics = llmServiceClient.getAllTopics();

        // 3. Sadece kullanıcının seçtiklerini filtrele
        return allTopics.stream()
                .filter(topic -> userTopicIds.contains(topic.getTopicId()))
                .collect(java.util.stream.Collectors.toList());
    }
}