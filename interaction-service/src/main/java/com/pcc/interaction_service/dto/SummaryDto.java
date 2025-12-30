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

    // Haberin kaynağına gitmek için url
    private String sourceUrl;

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
}