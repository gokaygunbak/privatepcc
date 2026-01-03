package com.pcc.interaction_service.repository;

import com.pcc.interaction_service.entity.UserInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserInteractionRepository extends JpaRepository<UserInteraction, UUID> {

        // Kullanıcının belirli bir içeriğe yaptığı etkileşimi bul (Örn: Beğenmiş mi?)
        java.util.List<UserInteraction> findByUserIdAndContentIdAndInteractionType(Long userId, UUID contentId,
                        UserInteraction.InteractionType type);

        // var mı yok mu kontrolü
        boolean existsByUserIdAndContentIdAndInteractionType(Long userId, UUID contentId,
                        UserInteraction.InteractionType type);

        // Kullanıcının kaydettiği içerikleri getir (SAVE tipi interaction'lar)
        java.util.List<UserInteraction> findByUserIdAndInteractionTypeOrderByCreatedAtDesc(Long userId,
                        UserInteraction.InteractionType type);

        // Tüm REPORT tipindeki etkileşimleri getir (Admin için)
        java.util.List<UserInteraction> findByInteractionTypeOrderByCreatedAtDesc(UserInteraction.InteractionType type);

        // Belirli bir içeriğin şikayet sayısını getir
        long countByContentIdAndInteractionType(UUID contentId, UserInteraction.InteractionType type);

        // Belirli bir içeriğe ait tüm interaction'ları sil
        void deleteByContentId(UUID contentId);

        // REPORT hariç tüm etkileşimleri say (LIKE, SAVE, VIEW)
        long countByInteractionTypeNot(UserInteraction.InteractionType type);
}