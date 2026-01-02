package com.pcc.interaction_service.dto;

public class TopicDto {

    private Integer topicId;
    private String name;

    public TopicDto() {
    }

    public TopicDto(Integer topicId, String name) {
        this.topicId = topicId;
        this.name = name;
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
}