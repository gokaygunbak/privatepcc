package com.pcc.llm_service.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "contents")
public class Content {
    @Id
    @Column(name = "content_id")
    private UUID contentId; // ID'yi biz üretmiyoruz, veritabanından okuyoruz

    @Column(name = "original_text", columnDefinition = "TEXT")
    private String originalText;

    @Column(name = "fetch_status")
    private String fetchStatus;
    @ManyToOne
    @JoinColumn(name = "source_id")
    private Source source;

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public UUID getContentId() {
        return contentId;
    }

    public void setContentId(UUID contentId) {
        this.contentId = contentId;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getFetchStatus() {
        return fetchStatus;
    }

    public void setFetchStatus(String fetchStatus) {
        this.fetchStatus = fetchStatus;
    }

    @jakarta.persistence.Column(name = "published_date")
    private java.time.LocalDateTime publishedDate;

    public java.time.LocalDateTime getPublishedDate() {
        return publishedDate;
    }

    public void setPublishedDate(java.time.LocalDateTime publishedDate) {
        this.publishedDate = publishedDate;
    }

    @jakarta.persistence.Column(name = "original_url", length = 2000)
    private String url; // Google News URL'leri çok uzun olabiliyor

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}