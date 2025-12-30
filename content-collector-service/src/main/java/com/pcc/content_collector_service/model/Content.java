package com.pcc.content_collector_service.model;

import jakarta.persistence.*;
//import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

//@Data
@Entity
@Table(name = "contents")
public class Content {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO) // UUID otomatik oluşsun
    private UUID contentId;

    private Integer sourceId;

    private String originalTitle;

    @Column(columnDefinition = "TEXT")
    private String originalText; // Haber metni uzun olabilir

    private String originalUrl;

    private LocalDateTime publishedDate;

    private String fetchStatus = "PENDING"; // Varsayılan: Bekliyor

    private LocalDateTime fetchedAt = LocalDateTime.now();

    public UUID getContentId() {
        return contentId;
    }

    public void setContentId(UUID contentId) {
        this.contentId = contentId;
    }

    public Integer getSourceId() {
        return sourceId;
    }

    public void setSourceId(Integer sourceId) {
        this.sourceId = sourceId;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public LocalDateTime getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(LocalDateTime publishedDate) {
        this.publishedDate = publishedDate;
    }

    public String getFetchStatus() {
        return fetchStatus;
    }

    public void setFetchStatus(String fetchStatus) {
        this.fetchStatus = fetchStatus;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(LocalDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }
}