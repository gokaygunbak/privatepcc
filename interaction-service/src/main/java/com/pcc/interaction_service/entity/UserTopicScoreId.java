package com.pcc.interaction_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.Objects;
import java.util.Objects;

@Data
// @NoArgsConstructor
// @AllArgsConstructor

public class UserTopicScoreId implements Serializable {
    private Long userId;
    private Integer topicId;

    public UserTopicScoreId() {
    }

    public UserTopicScoreId(Long userId, Integer topicId) {
        this.userId = userId;
        this.topicId = topicId;
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

    //Equals ve HashCode composite key için

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserTopicScoreId that = (UserTopicScoreId) o;
        return Objects.equals(userId, that.userId) &&
                Objects.equals(topicId, that.topicId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, topicId);
    }
}