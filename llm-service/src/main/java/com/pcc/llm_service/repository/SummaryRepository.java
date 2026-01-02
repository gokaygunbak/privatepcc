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

    // Pageable - tüm summary'leri sayfalı getir (published_date'e göre en yeni en üstte)
    @Query("SELECT s FROM Summary s ORDER BY s.content.publishedDate DESC NULLS LAST")
    Page<Summary> findAllByOrderByPublishedDateDesc(Pageable pageable);
    boolean existsByContent_ContentId(UUID contentId);

    // Topic'e göre summary'leri getir (Sadece Summary'nin topic_id'si - AI tarafından belirlenen)
    @Query("SELECT s FROM Summary s WHERE s.topic.topicId IN :topicIds ORDER BY s.createdAt DESC")
    List<Summary> findByTopicIdIn(@Param("topicIds") List<Integer> topicIds);

    // ContentId'den Summary'yi bul (Topic bilgisi için)
    @Query("SELECT s FROM Summary s WHERE s.content.contentId = :contentId")
    Summary findByContentId(@Param("contentId") UUID contentId);
}