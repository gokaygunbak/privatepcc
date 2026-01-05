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
        // 7. Yeni eklenen topic'lere puan ekle (Her zaman +5 ekle, varsa üstüne koy)
        for (Integer topicId : addedTopics) {
            System.out.println("➕ Topic=" + topicId + " tercih edildi. +5.0 puan ekleniyor.");
            updateUserTopicScore(userId, topicId, 5.0);
        }

        // 8. Korunan topic'lerin skorlarına DOKUNMA (mevcut skorları koru)
        System.out.println("✅ İşlem tamamlandı. Korunan topic'lerin skorları değişmedi.");
    }

    // Etkileşimi Kaydet ve Puanla
    @Transactional
    public void recordInteraction(InteractionRequest request) {
        // SAVE işlemi için özel kontrol: Varsa sil (Unsave), yoksa kaydet
        if (request.getInteractionType() == UserInteraction.InteractionType.SAVE && request.getContentId() != null) {
            java.util.List<UserInteraction> existingInteractions = interactionRepository
                    .findByUserIdAndContentIdAndInteractionType(
                            request.getUserId(),
                            request.getContentId(),
                            UserInteraction.InteractionType.SAVE);

            if (!existingInteractions.isEmpty()) {
                // VARSA -> HEPSİNİ SİL (Cleanup + Unsave) + PUAN DÜŞ
                interactionRepository.deleteAll(existingInteractions);
                System.out.println("🗑️ Unsave işlemi: " + existingInteractions.size() + " interaction silindi.");

                // Topic ID belirle
                Integer topicId = request.getTopicId();
                if (topicId == null) {
                    try {
                        topicId = llmServiceClient.getTopicIdByContentId(request.getContentId());
                    } catch (Exception e) {
                        System.err.println("⚠️ Topic ID alınamadı (Unsave): " + e.getMessage());
                    }
                }

                if (topicId != null) {
                    // Puanı geri al (Negatif skor) - Sadece BIR KERE ve EĞER SKOR VARSA
                    UserTopicScoreId scoreId = new UserTopicScoreId(request.getUserId(), topicId);
                    if (scoreRepository.existsById(scoreId)) {
                        double scoreDeduct = -1.0 * getScoreByInteractionType(UserInteraction.InteractionType.SAVE);
                        updateUserTopicScore(request.getUserId(), topicId, scoreDeduct);
                        System.out.println("📉 Puan düşüldü: " + scoreDeduct);
                    } else {
                        System.out.println("⚠️ Topic score bulunamadı (Topic=" + topicId + "), puan düşülmedi.");
                    }
                }

                return; // İşlem tamam, çık
            }
        }

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
                System.out.println("🎯 Topic ID LLM Service'den alındı: " + topicId + " (ContentId: "
                        + request.getContentId() + ")");
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

            // Özel Logic: Eğer scoreDelta -100 ise -> RESET (0 yap)
            // Eğer scoreDelta -0.7 ise -> %70 AZALT (0.3 ile çarp)
            if (scoreDelta == -100.0) {
                newScore = 0.0;
                System.out.println("⛔️ SKOR SIFIRLANDI (NOT_INTERESTED): User=" + userId + ", Topic=" + topicId);
            } else if (scoreDelta == -0.7) {
                newScore = currentScore * 0.3; // %70 azalt
                System.out.println("📉 SKOR AZALTILDI (SHOW_LESS): " + currentScore + " -> " + newScore);
            } else {
                newScore = currentScore + scoreDelta;
                // Skorun eksiye düşmesini engelle
                if (newScore < 0) {
                    newScore = 0.0;
                }
            }

            scoreEntity.setScore(newScore);
        } else {
            scoreEntity = new UserTopicScore();
            scoreEntity.setUserId(userId);
            scoreEntity.setTopicId(topicId);
            currentScore = 0.0;

            if (scoreDelta == -100.0 || scoreDelta == -0.7) {
                newScore = 0.0; // Yeni topic ise zaten 0
            } else {
                newScore = scoreDelta;
            }
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
            case CLICK -> 0.3;
            case SHOW_LESS -> -0.7; // %70 azalt
            case NOT_INTERESTED -> -100.0; // Sıfırla
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
        List<SummaryDto> personalizedFeed = buildWeightedFeed(userScores, summariesByTopic, totalScore,
                allSummaries.size());

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

            if (selectedTopicId == null)
                continue;

            List<SummaryDto> topicSummaries = summariesByTopic.get(selectedTopicId);
            if (topicSummaries == null)
                continue;

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

        // 2. Unique contentId'leri çıkar (aynı içerik birden fazla şikayet edilmiş
        // olabilir)
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

        // 1. Bu içeriğe ait tüm interaction'ları sil (LIKE, SAVE, REPORT)
        interactionRepository.deleteByContentId(contentId);
        System.out.println("   ✓ Interaction'lar silindi");

        // 2. LLM Service'e içeriği silmesini söyle
        llmServiceClient.deleteContent(contentId);
        System.out.println("   ✓ İçerik LLM Service'den silindi");
    }

    // Admin: Şikayeti Yoksay (Sadece REPORT interaction'larını sil)
    @Transactional
    public void dismissReport(java.util.UUID contentId) {
        System.out.println("🛡️ Şikayet yoksayılıyor: " + contentId);
        interactionRepository.deleteByContentIdAndInteractionType(contentId, UserInteraction.InteractionType.REPORT);
        System.out.println("   ✓ REPORT interaction'ları silindi.");
    }

    // Ağırlıklı Rastgele ve Görülmemiş İçerik Seçimi (Sonsuz Kaydırma İçin)
    public SummaryDto getNextWeightedContent(Long userId, boolean forceTop) {
        // 1. Kullanıcının skorlarını çek
        List<UserTopicScore> userScores = scoreRepository.findByUserIdOrderByScoreDesc(userId);

        Integer targetTopicId = null;

        // 2. Hedef Topic Belirle
        double totalScore = userScores.stream().mapToDouble(UserTopicScore::getScore).sum();

        if (userScores.isEmpty() || totalScore <= 0) {
            System.out.println(
                    "ℹ️ Kullanıcı skoru yok veya toplam skor 0 (Total=" + totalScore + "), rastgele seçim yapılacak.");
            // targetTopicId = null kalır -> Global Random
        } else if (forceTop) {
            // En yüksek skorlu konuyu zorla
            targetTopicId = userScores.get(0).getTopicId();
            System.out.println("🥇 Force Top aktif: Topic " + targetTopicId + " seçildi.");
        } else {
            // Ağırlıklı rastgele seçim yap
            java.util.Random random = new java.util.Random();
            targetTopicId = selectWeightedTopic(userScores, totalScore, random);
            System.out.println("🎲 Ağırlıklı seçim: Topic " + targetTopicId + " (Total Score: " + totalScore + ")");
        }

        // 3. LLM Service'den bu topic için içerik iste
        SummaryDto summary = null;
        if (targetTopicId != null) {
            summary = llmServiceClient.getRandomUnseenContent(userId, targetTopicId);
        }

        // 4. Fallback: Eğer seçilen topic'te içerik kalmadıysa veya topic null ise
        if (summary == null) {
            System.out.println(
                    "⚠️ Topic " + targetTopicId + " için içerik kalmadı veya bulunamadı. Fallback: Global Random.");
            // TopicID olmadan (null) global random iste
            summary = llmServiceClient.getRandomUnseenContent(userId, null);
        }

        return summary;
    }

    // Kullanıcının Topic İstatistiklerini Getir
    public List<com.pcc.interaction_service.dto.TopicScoreDto> getUserTopicStats(Long userId) {
        List<UserTopicScore> userScores = scoreRepository.findByUserIdOrderByScoreDesc(userId);

        if (userScores.isEmpty()) {
            return List.of();
        }

        // 1. Toplam skoru hesapla
        double totalScore = userScores.stream()
                .mapToDouble(UserTopicScore::getScore)
                .sum();

        // 2. Tüm konuları çek (isimleri almak için)
        List<com.pcc.interaction_service.dto.TopicDto> allTopics = llmServiceClient.getAllTopics();
        java.util.Map<Integer, String> topicNames = allTopics.stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.pcc.interaction_service.dto.TopicDto::getTopicId,
                        com.pcc.interaction_service.dto.TopicDto::getName));

        // 3. DTO'ları oluştur
        List<com.pcc.interaction_service.dto.TopicScoreDto> stats = new ArrayList<>();
        for (UserTopicScore score : userScores) {
            double percentage = (totalScore > 0) ? (score.getScore() / totalScore) * 100 : 0;
            String name = topicNames.getOrDefault(score.getTopicId(), "Bilinmeyen Konu");

            stats.add(new com.pcc.interaction_service.dto.TopicScoreDto(
                    score.getTopicId(),
                    name,
                    score.getScore(),
                    percentage));
        }

        return stats;
    }

    // Kullanıcının Tüm Verilerini Sıfırla (Reset Algorithm)
    @Transactional
    public void resetUserAlgorithm(Long userId) {
        System.out.println("🧨 ALGORİTMA SIFIRLANIYOR: User=" + userId);

        // 1. Tüm Interaction'ları sil
        interactionRepository.deleteByUserId(userId);
        System.out.println("   ✓ Interaction'lar silindi.");

        // 2. Tüm Topic Skorlarını sil
        scoreRepository.deleteByUserId(userId);
        System.out.println("   ✓ Skorlar silindi.");

        // 3. Tüm Tercihleri sil
        preferenceRepository.deleteAllByUserId(userId);
        System.out.println("   ✓ Tercihler silindi.");

        System.out.println("✅ Kullanıcı verileri tamamen temizlendi.");
    }
}