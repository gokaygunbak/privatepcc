package com.pcc.llm_service.dto;

public class TopicStatsDto {
    private Integer topicId;
    private String name;
    private Long contentCount;

    public TopicStatsDto(Integer topicId, String name, Long contentCount) {
        this.topicId = topicId;
        this.name = name;
        this.contentCount = contentCount;
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

    public Long getContentCount() {
        return contentCount;
    }

    public void setContentCount(Long contentCount) {
        this.contentCount = contentCount;
    }
}
