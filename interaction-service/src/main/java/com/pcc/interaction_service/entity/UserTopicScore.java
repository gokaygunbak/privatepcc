package com.pcc.interaction_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_topic_scores")
@IdClass(UserTopicScoreId.class)
@Data
public class UserTopicScore {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "topic_id")
    private Integer topicId;

    private Double score = 0.0; // Varsayılan başlangıç puanı

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated = LocalDateTime.now();

    public UserTopicScore() {
    }

    public UserTopicScore(Long userId, Integer topicId, Double score) {
        this.userId = userId;
        this.topicId = topicId;
        this.score = score;
        this.lastUpdated = LocalDateTime.now();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getTopicId() {
        return topicId;
    }

    public void setTopicId(Integer topicId) {
        this.topicId = topicId;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}