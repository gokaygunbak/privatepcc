package com.pcc.llm_service.repository;

import com.pcc.llm_service.model.Summary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, UUID> {

        // Pageable - tüm summary'leri sayfalı getir (published_date'e göre en yeni en
        // üstte)
        @Query("SELECT s FROM Summary s ORDER BY s.content.publishedDate DESC NULLS LAST")
        Page<Summary> findAllByOrderByPublishedDateDesc(Pageable pageable);

        boolean existsByContent_ContentId(UUID contentId);

        // Topic'e göre summary'leri getir (Sadece Summary'nin topic_id'si - AI
        // tarafından belirlenen)
        @Query("SELECT s FROM Summary s WHERE s.topic.topicId IN :topicIds ORDER BY s.createdAt DESC")
        List<Summary> findByTopicIdIn(@Param("topicIds") List<Integer> topicIds);

        // ContentId'den Summary'yi bul (Topic bilgisi için)
        @Query("SELECT s FROM Summary s WHERE s.content.contentId = :contentId")
        Summary findByContentId(@Param("contentId") UUID contentId);

        // Rastgele ve daha önce görülmemiş bir içerik getir (Native Query - PostgreSQL)
        @Query(value = "SELECT * FROM summaries s " +
                        "WHERE s.content_id NOT IN (SELECT v.content_id FROM view_history v WHERE v.user_id = :userId) "
                        +
                        "ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
        java.util.Optional<Summary> findRandomUnseenSummary(@Param("userId") Long userId);

        // Konuya özel Rastgele ve daha önce görülmemiş içerik getir
        @Query(value = "SELECT * FROM summaries s " +
                        "WHERE s.topic_id = :topicId " +
                        "AND s.content_id NOT IN (SELECT v.content_id FROM view_history v WHERE v.user_id = :userId) " +
                        "ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
        java.util.Optional<Summary> findRandomUnseenSummaryByTopic(@Param("userId") Long userId,
                        @Param("topicId") Integer topicId);

        // --- Trends Page / Random Feed Methods ---

        // 1. Toplam görülmemiş içerik sayısı (Pagination hesaplaması için)
        @Query(value = "SELECT COUNT(*) FROM summaries s " +
                        "WHERE s.content_id NOT IN (SELECT v.content_id FROM view_history v WHERE v.user_id = :userId)", nativeQuery = true)
        long countUnseenSummaries(@Param("userId") Long userId);

        // 2. Rastgele görülmemiş içerik getir (Exclusion listesi yoksa)
        @Query(value = "SELECT * FROM summaries s " +
                        "WHERE s.content_id NOT IN (SELECT v.content_id FROM view_history v WHERE v.user_id = :userId) "
                        +
                        "ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
        List<Summary> findRandomUnseenSummaries(@Param("userId") Long userId, @Param("limit") int limit);

        // 3. Rastgele görülmemiş içerik getir (Exclusion listesi varsa)
        @Query(value = "SELECT * FROM summaries s " +
                        "WHERE s.content_id NOT IN (SELECT v.content_id FROM view_history v WHERE v.user_id = :userId) "
                        +
                        "AND s.content_id NOT IN (:excludeIds) " +
                        "ORDER BY RANDOM() LIMIT :limit", nativeQuery = true)
        List<Summary> findRandomUnseenSummariesWithExclusion(@Param("userId") Long userId,
                        @Param("excludeIds") List<UUID> excludeIds, @Param("limit") int limit);

        // 4. Konulara göre içerik sayılarını getir (Admin Paneli İçin)
        @Query("SELECT new com.pcc.llm_service.dto.TopicStatsDto(s.topic.topicId, s.topic.name, COUNT(s)) " +
                        "FROM Summary s " +
                        "WHERE s.topic IS NOT NULL " +
                        "GROUP BY s.topic.topicId, s.topic.name")
        List<com.pcc.llm_service.dto.TopicStatsDto> countSummariesByTopic();
}