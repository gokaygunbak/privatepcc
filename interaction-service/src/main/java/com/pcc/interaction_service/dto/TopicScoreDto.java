package com.pcc.interaction_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
// @AllArgsConstructor
public class TopicScoreDto {
    private Integer topicId;
    private String topicName;
    private Double score;
    private Double percentage;

    public TopicScoreDto(Integer topicId, String topicName, Double score, Double percentage) {
        this.topicId = topicId;
        this.topicName = topicName;
        this.score = score;
        this.percentage = percentage;
    }

    public Integer getTopicId() {
        return topicId;
    }

    public void setTopicId(Integer topicId) {
        this.topicId = topicId;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }
}
