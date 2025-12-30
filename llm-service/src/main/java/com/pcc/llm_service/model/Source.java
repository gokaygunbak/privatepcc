package com.pcc.llm_service.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sources")
@Data
public class Source {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer sourceId;

    private String name;

    private String url;

    // Source -> Topic'e bağlanır.
    // Sorgudaki 'src.topic' kısmı burasıdır.
    @ManyToOne
    @JoinColumn(name = "topic_id")
    private Topic topic;

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

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }
}