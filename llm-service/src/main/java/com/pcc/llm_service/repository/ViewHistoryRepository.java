package com.pcc.llm_service.repository;

import com.pcc.llm_service.model.ViewHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ViewHistoryRepository extends JpaRepository<ViewHistory, UUID> {
}
