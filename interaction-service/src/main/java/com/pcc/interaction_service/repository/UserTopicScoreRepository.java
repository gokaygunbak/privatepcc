package com.pcc.interaction_service.repository;

import com.pcc.interaction_service.entity.UserTopicScore;
import com.pcc.interaction_service.entity.UserTopicScoreId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTopicScoreRepository extends JpaRepository<UserTopicScore, UserTopicScoreId> {

    // Kullanıcının en çok puan topladığı konuları (Skora göre azalan) getir
    // Pageable ile sınırlandırabiliriz ama şimdilik listeleyelim
    List<UserTopicScore> findByUserIdOrderByScoreDesc(Long userId);

    // Belli bir eşiğin üstündeki konuları getir (Örn: Skoru 5'ten büyük olanlar
    // favoridir)
    @Query("SELECT u.topicId FROM UserTopicScore u WHERE u.userId = :userId AND u.score > :threshold")
    List<Integer> findTopTopicIds(Long userId, Double threshold);
}