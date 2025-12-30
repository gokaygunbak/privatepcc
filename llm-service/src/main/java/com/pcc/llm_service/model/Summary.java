package com.pcc.llm_service.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "summaries")
public class Summary {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID summaryId;

    @OneToOne
    @JoinColumn(name = "content_id", referencedColumnName = "content_id")
    private Content content; // Hangi haberin özeti?

    private String title; // AI'nın attığı başlık

    @Column(columnDefinition = "TEXT")
    private String summaryText; // Özet

    private String generatedTags; // Etiketler

    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getSummaryId() {
        return summaryId;
    }

    public void setSummaryId(UUID summaryId) {
        this.summaryId = summaryId;
    }

    public Content getContent() {
        return content;
    }

    public void setContent(Content content) {
        this.content = content;
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

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // JSON'a "topicId" alanını eklemek için sanal getter
    public Integer getTopicId() {
        if (content != null && content.getSource() != null && content.getSource().getTopic() != null) {
            return content.getSource().getTopic().getTopicId();
        }
        return null;
    }

    // JSON'a "sourceUrl" alanını eklemek için sanal getter (Frontend istiyor)
    public String getSourceUrl() {
        if (content != null) {
            return content.getUrl();
        }
        return null;
    }
    // ---------------------------
}