package com.pcc.llm_service.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "topics")
@Data
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer topicId;

    private String name;

    private String keywords;

    public Topic() {
    }

    public Topic(String name, String keywords) {
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