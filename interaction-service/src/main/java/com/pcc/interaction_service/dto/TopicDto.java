package com.pcc.interaction_service.dto;

import lombok.Data;

public class TopicDto {

    private Integer topicId;
    private String name;
    private String keywords;

    public TopicDto() {
    }

    public TopicDto(Integer topicId, String name, String keywords) {
        this.topicId = topicId;
        this.name = name;
        this.keywords = keywords;
    }

    public Integer getTopicId() {
        return topicId;
    }

    public void setTopicId(Integer topicId) {
        this.topicId = topicId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
}