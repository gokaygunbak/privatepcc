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

    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic; // AI tarafından belirlenen konu

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

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    // JSON'a "topicId" alanını eklemek için getter (AI'ın belirlediği topic)
    public Integer getTopicId() {
        if (topic != null) {
            return topic.getTopicId();
        }
        return null;
    }

    // JSON'a "topicName" alanını eklemek için getter
    public String getTopicName() {
        if (topic != null) {
            return topic.getName();
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