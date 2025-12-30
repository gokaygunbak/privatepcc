package com.pcc.interaction_service.repository;

import com.pcc.interaction_service.entity.UserInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserInteractionRepository extends JpaRepository<UserInteraction, UUID> {

    // Kullanıcının belirli bir içeriğe yaptığı etkileşimi bul (Örn: Beğenmiş mi?)
    Optional<UserInteraction> findByUserIdAndContentIdAndInteractionType(Long userId, UUID contentId, UserInteraction.InteractionType type);

    //var mı yok mu kontrolü
    boolean existsByUserIdAndContentIdAndInteractionType(Long userId, UUID contentId, UserInteraction.InteractionType type);
}