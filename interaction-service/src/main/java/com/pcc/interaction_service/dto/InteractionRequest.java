package com.pcc.interaction_service.dto;

import com.pcc.interaction_service.entity.UserInteraction;
import lombok.Data;

import java.util.UUID;

@Data
public class InteractionRequest {
    private Long userId;
    private UUID contentId;
    private UserInteraction.InteractionType interactionType;
    private Integer topicId;
    public InteractionRequest() {
    }

    public InteractionRequest(Long userId, UUID contentId, UserInteraction.InteractionType interactionType, Integer topicId) {
        this.userId = userId;
        this.contentId = contentId;
        this.interactionType = interactionType;
        this.topicId = topicId;
    }


    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public UUID getContentId() {
        return contentId;
    }

    public void setContentId(UUID contentId) {
        this.contentId = contentId;
    }

    public UserInteraction.InteractionType getInteractionType() {
        return interactionType;
    }

    public void setInteractionType(UserInteraction.InteractionType interactionType) {
        this.interactionType = interactionType;
    }

    public Integer getTopicId() {
        return topicId;
    }

    public void setTopicId(Integer topicId) {
        this.topicId = topicId;
    }
}

