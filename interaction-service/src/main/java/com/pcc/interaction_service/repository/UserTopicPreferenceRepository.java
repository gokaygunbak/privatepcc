package com.pcc.interaction_service.repository;

import com.pcc.interaction_service.entity.UserTopicPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTopicPreferenceRepository extends JpaRepository<UserTopicPreference, Long> {

    // Kullanıcının tüm tercihlerini getir
    List<UserTopicPreference> findByUserId(Long userId);

    // Sadece Topic ID'lerini liste olarak getir
    @Query("SELECT u.topicId FROM UserTopicPreference u WHERE u.userId = :userId")
    List<Integer> findTopicIdsByUserId(Long userId);

    // Kullanıcı bu konuyu zaten takip ediyor mu? (Çift kayıt olmasın diye)
    boolean existsByUserIdAndTopicId(Long userId, Integer topicId);
}