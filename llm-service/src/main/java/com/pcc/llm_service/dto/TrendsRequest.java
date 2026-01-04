package com.pcc.llm_service.dto;

import java.util.List;
import java.util.UUID;

public class TrendsRequest {
    private Long userId;
    private List<UUID> excludeIds;
    private int size;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<UUID> getExcludeIds() {
        return excludeIds;
    }

    public void setExcludeIds(List<UUID> excludeIds) {
        this.excludeIds = excludeIds;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
