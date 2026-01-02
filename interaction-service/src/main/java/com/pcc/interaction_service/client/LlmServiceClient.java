package com.pcc.interaction_service.client;

import com.pcc.interaction_service.dto.SummaryDto;
import com.pcc.interaction_service.dto.TopicDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// name: Diğer servisin adı
// url: Diğer servisin adresi
@FeignClient(name = "llm-service", url = "http://localhost:8083")
public interface LlmServiceClient {

    // LLM Servisinden topic listesine göre haber isteme metodu
    @GetMapping("/api/llm/summaries/by-topics")
    List<SummaryDto> getSummariesByTopics(@RequestParam("topicIds") List<Integer> topicIds);

    // Tüm konuları getir (Kullanıcı seçim yapsın diye)
    @GetMapping("/api/llm/topics")
    List<TopicDto> getAllTopics();

    // ContentId'den Topic ID'yi getir (Like/Save işleminde puanlama için)
    @GetMapping("/api/llm/summaries/topic-by-content/{contentId}")
    Integer getTopicIdByContentId(@PathVariable("contentId") java.util.UUID contentId);

    // ContentId listesine göre summary'leri getir (Kaydedilen içerikler için)
    @GetMapping("/api/llm/summaries/by-contents")
    List<SummaryDto> getSummariesByContentIds(@RequestParam("contentIds") List<java.util.UUID> contentIds);

    // Admin: İçeriği sil (summary ve content)
    @DeleteMapping("/api/llm/content/{contentId}")
    void deleteContent(@PathVariable("contentId") java.util.UUID contentId);
}