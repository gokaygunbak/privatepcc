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

    // Kişiselleştirilmiş Akışı Getir (Ağırlıklı Rastgele Seçim Algoritması)
    public List<SummaryDto> getPersonalizedFeed(Long userId) {
        // 1. Kullanıcının topic skorlarını çek (en yüksekten en düşüğe)
        List<UserTopicScore> userScores = scoreRepository.findByUserIdOrderByScoreDesc(userId);
        
        if (userScores.isEmpty()) {
            System.out.println("📭 Kullanıcı " + userId + " için hiç skor bulunamadı.");
            return List.of();
        }

        // 2. Topic ID'lerini çıkar
        List<Integer> topicIds = userScores.stream()
                .map(UserTopicScore::getTopicId)
                .collect(java.util.stream.Collectors.toList());

        // 3. Toplam skoru hesapla
        double totalScore = userScores.stream()
                .mapToDouble(UserTopicScore::getScore)
                .sum();

        // 4. Yüzdelikleri hesapla ve logla
        System.out.println("📊 Kullanıcı " + userId + " için ağırlıklı dağılım:");
        for (UserTopicScore score : userScores) {
            double percentage = (score.getScore() / totalScore) * 100;
            System.out.println("   Topic " + score.getTopicId() + ": " + 
                    String.format("%.1f", score.getScore()) + " puan → %" + 
                    String.format("%.1f", percentage));
        }

        // 5. LLM Servisinden bu topic'lere ait içerikleri al
        List<SummaryDto> allSummaries = llmServiceClient.getSummariesByTopics(topicIds);

        if (allSummaries.isEmpty()) {
            System.out.println("📭 Bu topic'lere ait içerik bulunamadı.");
            return List.of();
        }

        // 6. İçerikleri topic'lerine göre grupla
        java.util.Map<Integer, List<SummaryDto>> summariesByTopic = allSummaries.stream()
                .filter(s -> s.getTopicId() != null)
                .collect(java.util.stream.Collectors.groupingBy(SummaryDto::getTopicId));

        // 7. Ağırlıklı rastgele seçim ile feed oluştur
        List<SummaryDto> personalizedFeed = buildWeightedFeed(userScores, summariesByTopic, totalScore, allSummaries.size());

        System.out.println("✅ " + personalizedFeed.size() + " içerik ağırlıklı algoritma ile sıralandı.");
        return personalizedFeed;
    }

    /**
     * Ağırlıklı Rastgele Seçim Algoritması
     * - İlk içerik: Kesinlikle en yüksek skorlu topic'ten
     * - Sonraki içerikler: Skorlara göre yüzdelik olasılıkla seçilir
     */
    private List<SummaryDto> buildWeightedFeed(
            List<UserTopicScore> userScores,
            java.util.Map<Integer, List<SummaryDto>> summariesByTopic,
            double totalScore,
            int maxItems) {
        
        List<SummaryDto> result = new java.util.ArrayList<>();
        java.util.Random random = new java.util.Random();
        
        // Her topic için kullanılan index'leri takip et (aynı içerik tekrar gelmesin)
        java.util.Map<Integer, Integer> topicIndices = new java.util.HashMap<>();
        for (Integer topicId : summariesByTopic.keySet()) {
            topicIndices.put(topicId, 0);
        }

        // İlk içerik: Kesinlikle en yüksek skorlu topic'ten
        if (!userScores.isEmpty()) {
            Integer topTopicId = userScores.get(0).getTopicId();
            List<SummaryDto> topTopicSummaries = summariesByTopic.get(topTopicId);
            if (topTopicSummaries != null && !topTopicSummaries.isEmpty()) {
                result.add(topTopicSummaries.get(0));
                topicIndices.put(topTopicId, 1);
                System.out.println("🥇 İlk içerik: Topic " + topTopicId + " (En yüksek skor)");
            }
        }

        // Kalan içerikler: Ağırlıklı rastgele seçim
        int attempts = 0;
        int maxAttempts = maxItems * 3; // Sonsuz döngüyü önle
        
        while (result.size() < maxItems && attempts < maxAttempts) {
            attempts++;
            
            // Rastgele bir topic seç (skorlara göre ağırlıklı)
            Integer selectedTopicId = selectWeightedTopic(userScores, totalScore, random);
            
            if (selectedTopicId == null) continue;
            
            List<SummaryDto> topicSummaries = summariesByTopic.get(selectedTopicId);
            if (topicSummaries == null) continue;
            
            int currentIndex = topicIndices.getOrDefault(selectedTopicId, 0);
            
            // Bu topic'te hala içerik var mı?
            if (currentIndex < topicSummaries.size()) {
                SummaryDto summary = topicSummaries.get(currentIndex);
                
                // Daha önce eklenmemişse ekle
                if (!result.contains(summary)) {
                    result.add(summary);
                    topicIndices.put(selectedTopicId, currentIndex + 1);
                }
            }
        }

        return result;
    }

    /**
     * Skorlara göre ağırlıklı topic seçimi
     * Örnek: Futbol 35, Motor 10, Gastro 5 → Toplam 50
     * Random 0-50 arası: 0-35 → Futbol, 35-45 → Motor, 45-50 → Gastro
     */
    private Integer selectWeightedTopic(List<UserTopicScore> userScores, double totalScore, java.util.Random random) {
        double randomValue = random.nextDouble() * totalScore;
        double cumulative = 0;
        
        for (UserTopicScore score : userScores) {
            cumulative += score.getScore();
            if (randomValue <= cumulative) {
                return score.getTopicId();
            }
        }
        
        // Fallback: İlk topic
        return userScores.isEmpty() ? null : userScores.get(0).getTopicId();
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

    // Kullanıcının Kaydettiği İçerikleri Getir
    public List<SummaryDto> getSavedContents(Long userId) {
        // 1. Kullanıcının SAVE tipi interaction'larını al (en son kaydedilen en üstte)
        List<UserInteraction> savedInteractions = interactionRepository
                .findByUserIdAndInteractionTypeOrderByCreatedAtDesc(userId, UserInteraction.InteractionType.SAVE);

        if (savedInteractions.isEmpty()) {
            System.out.println("📭 Kullanıcı " + userId + " hiç içerik kaydetmemiş.");
            return List.of();
        }

        // 2. ContentId'leri sıralı olarak çıkar (kaydetme sırasına göre)
        List<java.util.UUID> contentIds = savedInteractions.stream()
                .map(UserInteraction::getContentId)
                .collect(java.util.stream.Collectors.toList());

        System.out.println("📚 Kullanıcı " + userId + " için " + contentIds.size() + " kayıtlı içerik bulundu.");

        // 3. LLM Service'den summary'leri çek
        List<SummaryDto> summaries = llmServiceClient.getSummariesByContentIds(contentIds);

        // 4. Summary'leri kaydetme sırasına göre sırala (contentIds sırasına göre)
        java.util.Map<java.util.UUID, SummaryDto> summaryMap = summaries.stream()
                .filter(s -> s.getContent() != null && s.getContent().getContentId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        s -> s.getContent().getContentId(),
                        s -> s,
                        (existing, replacement) -> existing // duplicate durumunda ilkini tut
                ));

        // ContentIds sırasına göre summary'leri döndür
        return contentIds.stream()
                .map(summaryMap::get)
                .filter(s -> s != null)
                .collect(java.util.stream.Collectors.toList());
    }

    // Admin: Şikayet Edilen İçerikleri Getir
    public List<SummaryDto> getReportedContents() {
        // 1. Tüm REPORT tipindeki interaction'ları al
        List<UserInteraction> reportInteractions = interactionRepository
                .findByInteractionTypeOrderByCreatedAtDesc(UserInteraction.InteractionType.REPORT);

        if (reportInteractions.isEmpty()) {
            System.out.println("📭 Hiç şikayet edilen içerik yok.");
            return List.of();
        }

        // 2. Unique contentId'leri çıkar (aynı içerik birden fazla şikayet edilmiş olabilir)
        List<java.util.UUID> contentIds = reportInteractions.stream()
                .map(UserInteraction::getContentId)
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        System.out.println("⚠️ " + contentIds.size() + " farklı içerik şikayet edilmiş.");

        // 3. LLM Service'den summary'leri çek
        List<SummaryDto> summaries = llmServiceClient.getSummariesByContentIds(contentIds);

        // 4. Her summary'ye şikayet sayısını ekle (DTO'da reportCount alanı varsa)
        return summaries;
    }

    // Admin: İçeriği ve İlişkili Tüm Verileri Sil
    @Transactional
    public void deleteContentCompletely(java.util.UUID contentId) {
        System.out.println("🗑️ İçerik siliniyor: " + contentId);

        // 1. Bu içeriğe ait tüm interaction'ları sil (LIKE, SAVE, VIEW, REPORT)
        interactionRepository.deleteByContentId(contentId);
        System.out.println("   ✓ Interaction'lar silindi");

        // 2. LLM Service'e içeriği silmesini söyle
        llmServiceClient.deleteContent(contentId);
        System.out.println("   ✓ İçerik LLM Service'den silindi");
    }
}