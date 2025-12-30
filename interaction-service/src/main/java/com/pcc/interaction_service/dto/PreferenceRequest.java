package com.pcc.interaction_service.dto;
import java.util.List;



public class PreferenceRequest {
    private Long userId;
    private List<Integer> topicIds;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<Integer> getTopicIds() {
        return topicIds;
    }

    public void setTopicIds(List<Integer> topicIds) {
        this.topicIds = topicIds;
    }
}
