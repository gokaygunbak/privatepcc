package com.pcc.content_collector_service.repository;

import com.pcc.content_collector_service.model.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SourceRepository extends JpaRepository<Source, Integer> {

}