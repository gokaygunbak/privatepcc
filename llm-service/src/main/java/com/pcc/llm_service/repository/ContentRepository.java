package com.pcc.llm_service.repository;

import com.pcc.llm_service.model.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID> {
    List<Content> findByFetchStatus(String status);
}