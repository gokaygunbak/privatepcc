package com.pcc.llm_service.repository;

import com.pcc.llm_service.model.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TopicRepository extends JpaRepository<Topic, Integer> {}