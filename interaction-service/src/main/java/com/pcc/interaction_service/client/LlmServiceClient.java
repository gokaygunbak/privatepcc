package com.pcc.interaction_service.client;

import com.pcc.interaction_service.dto.SummaryDto;
import com.pcc.interaction_service.dto.TopicDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

// name: Diğer servisin adı (önemsiz ama standart)
// url: Diğer servisin adresi (LLM Servisi 8083'te çalışıyor)
@FeignClient(name = "llm-service", url = "http://localhost:8083")
public interface LlmServiceClient {

    // 1. LLM Servisinden topic listesine göre haber isteme metodu
    // (Bunu birazdan LLM servisine ekleyeceğiz, şu an hattın ucu boşta)
    @GetMapping("/api/llm/summaries/by-topics")
    List<SummaryDto> getSummariesByTopics(@RequestParam("topicIds") List<Integer> topicIds);

    // 2. Tüm konuları getir (Kullanıcı seçim yapsın diye)
    @GetMapping("/api/llm/topics")
    List<TopicDto> getAllTopics();
}