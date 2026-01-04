package com.pcc.interaction_service.repository;

import com.pcc.interaction_service.entity.UserTopicScore;
import com.pcc.interaction_service.entity.UserTopicScoreId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTopicScoreRepository extends JpaRepository<UserTopicScore, UserTopicScoreId> {

    // Pageable ile sınırlandırabilirim ama şimdilik listeleyelim
    List<UserTopicScore> findByUserIdOrderByScoreDesc(Long userId);

    // Belli bir eşiğin üstündeki konuları getir Skoru 5'ten büyük olanlar vs
    @Query("SELECT u.topicId FROM UserTopicScore u WHERE u.userId = :userId AND u.score > :threshold")
    List<Integer> findTopTopicIds(Long userId, Double threshold);

    // Kullanıcının tüm skorlarını sil (Onboarding tekrarında)
    void deleteAllByUserId(Long userId);

    // Alias for consistency
    void deleteByUserId(Long userId);
}