package com.pcc.llm_service.dto;

import com.pcc.llm_service.model.Summary;
import java.util.List;

public class TrendsResponse {
    private List<Summary> content;
    private int totalPages;
    private long totalElements;

    public TrendsResponse(List<Summary> content, int totalPages, long totalElements) {
        this.content = content;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
    }

    public List<Summary> getContent() {
        return content;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }
}
