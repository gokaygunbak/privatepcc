package com.pcc.content_collector_service.model;

import jakarta.persistence.*;
//import lombok.Data;
import java.time.LocalDateTime;

//@Data
@Entity
@Table(name = "sources")
public class Source {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer sourceId;

    private String name;

    @Column(nullable = false)
    private String url; // RSS Linki burası

    private Integer topicId; // Hangi konuya ait?

    private LocalDateTime lastFetchedAt;

    public Integer getSourceId() {
        return sourceId;
    }

    public void setSourceId(Integer sourceId) {
        this.sourceId = sourceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getTopicId() {
        return topicId;
    }

    public void setTopicId(Integer topicId) {
        this.topicId = topicId;
    }

    public LocalDateTime getLastFetchedAt() {
        return lastFetchedAt;
    }

    public void setLastFetchedAt(LocalDateTime lastFetchedAt) {
        this.lastFetchedAt = lastFetchedAt;
    }
}