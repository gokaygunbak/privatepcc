package com.pcc.content_collector_service.repository;

import com.pcc.content_collector_service.model.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface ContentRepository extends JpaRepository<Content, UUID> {
    boolean existsByOriginalUrl(String originalUrl);

    // Anahtar kelimeye göre arama Büyük küçük harf duyarsız
    java.util.List<Content> findByOriginalTitleContainingIgnoreCaseOrOriginalTextContainingIgnoreCase(String title,
            String text);
}