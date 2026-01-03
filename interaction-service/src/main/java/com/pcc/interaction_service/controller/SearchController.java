package com.pcc.interaction_service.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.UUID;
import lombok.Data;

@RestController
@RequestMapping("/api/interactions")
public class SearchController {

    @Autowired
    private RestTemplate restTemplate;

    // TODO: Feign Client kullanmak daha temiz olur ama mevcut mimaride RestTemplate
    // mi kullanılıyor kontrol etmeli.
    // Şimdilik hızlı çözüm olarak RestTemplate.
    private final String COLLECTOR_SERVICE_URL = "http://localhost:8081/api/collector";
    private final String LLM_SERVICE_URL = "http://localhost:8083/api/llm";

    @PostMapping("/search")
    public ResponseEntity<List<SummaryDTO>> searchAndSummarize(@RequestParam String query) {
        System.out.println("Arama İsteği Geldi: " + query);

        // 1. Content Collector'dan içerikleri bul
        // GET /search?query=... return List<Content>
        String searchUrl = COLLECTOR_SERVICE_URL + "/search?query=" + query;
        ResponseEntity<List<ContentDTO>> contentResponse = restTemplate.exchange(
                searchUrl,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<ContentDTO>>() {
                });

        List<ContentDTO> contents = contentResponse.getBody();
        if (contents == null || contents.isEmpty()) {
            System.out.println("Collector'da içerik bulunamadı.");
            return ResponseEntity.ok(List.of());
        }

        System.out.println("Bulunan içerik sayısı: " + contents.size());

        // 2. Bulunan içeriklerin ID'lerini al
        List<UUID> contentIds = contents.stream()
                .map(ContentDTO::getContentId)
                .collect(Collectors.toList());

        // 3. LLM Service'e gönder ve özetle/getir
        // POST /summarize-batch return List<Summary>
        // RestTemplate call
        String summarizeUrl = LLM_SERVICE_URL + "/summarize-batch";
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        org.springframework.http.HttpEntity<List<UUID>> requestEntity = new org.springframework.http.HttpEntity<>(
                contentIds, headers);

        ResponseEntity<List<SummaryDTO>> summaryResponse = restTemplate.exchange(
                summarizeUrl,
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<List<SummaryDTO>>() {
                });

        return ResponseEntity.ok(summaryResponse.getBody());
    }

    // DTOs (Basit iç sınıflar olarak tanımlıyorum şimdilik)
    @Data
    public static class ContentDTO {
        private UUID contentId;
        private String title;
        private String originalText;

        public UUID getContentId() {
            return contentId;
        }

        public void setContentId(UUID contentId) {
            this.contentId = contentId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getOriginalText() {
            return originalText;
        }

        public void setOriginalText(String originalText) {
            this.originalText = originalText;
        }

        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    @Data
    public static class SummaryDTO {
        private UUID summaryId;
        private String title;
        private String summaryText;
        private String generatedTags;
        private java.time.LocalDateTime createdAt;
        private ContentDTO content; // İlişkili içerik
        private Integer topicId; // Frontend interaction için gerekebilir, aslında ContentDTO'da olmalı

        public UUID getSummaryId() {
            return summaryId;
        }

        public void setSummaryId(UUID summaryId) {
            this.summaryId = summaryId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSummaryText() {
            return summaryText;
        }

        public void setSummaryText(String summaryText) {
            this.summaryText = summaryText;
        }

        public String getGeneratedTags() {
            return generatedTags;
        }

        public void setGeneratedTags(String generatedTags) {
            this.generatedTags = generatedTags;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public ContentDTO getContent() {
            return content;
        }

        public void setContent(ContentDTO content) {
            this.content = content;
        }

        public Integer getTopicId() {
            return topicId;
        }

        public void setTopicId(Integer topicId) {
            this.topicId = topicId;
        }
    }
}
