package com.pcc.interaction_service.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

public class SummaryDto {
    private UUID summaryId;
    private String title;
    private String summaryText;
    private String generatedTags;
    private LocalDateTime createdAt;

    // Haberin konusu puanlama için
    private Integer topicId;
    private String topicName;

    // Haberin kaynağına gitmek için url
    private String sourceUrl;

    // Content bilgisi (kaydetme sıralaması için)
    private ContentDto content;

    public SummaryDto() {
    }

    public UUID getSummaryId() {
        return summaryId;
    }

    public void setSummaryId(UUID summaryId) {
        this.summaryId = summaryId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public String getGeneratedTags() {
        return generatedTags;
    }

    public void setGeneratedTags(String generatedTags) {
        this.generatedTags = generatedTags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getTopicId() {
        return topicId;
    }

    public void setTopicId(Integer topicId) {
        this.topicId = topicId;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public ContentDto getContent() {
        return content;
    }

    public void setContent(ContentDto content) {
        this.content = content;
    }

    // Nested Content DTO
    public static class ContentDto {
        private UUID contentId;
        private String url;

        public UUID getContentId() {
            return contentId;
        }

        public void setContentId(UUID contentId) {
            this.contentId = contentId;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}