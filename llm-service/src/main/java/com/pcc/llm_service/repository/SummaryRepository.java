package com.pcc.llm_service.repository;

import com.pcc.llm_service.model.Summary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;

@Repository
public interface SummaryRepository extends JpaRepository<Summary, UUID> {
    boolean existsByContent_ContentId(UUID contentId);

    @Query("SELECT s FROM Summary s " +
            "JOIN s.content c " +
            "JOIN c.source src " +
            "WHERE src.topic.topicId IN :topicIds " +
            "ORDER BY s.createdAt DESC")
    List<Summary> findByTopicIdIn(@Param("topicIds") List<Integer> topicIds);
}