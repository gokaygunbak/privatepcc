package com.pcc.llm_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcc.llm_service.model.Content;
import com.pcc.llm_service.model.Summary;
import com.pcc.llm_service.model.Topic;
import com.pcc.llm_service.repository.ContentRepository;
import com.pcc.llm_service.repository.SummaryRepository;

import com.pcc.llm_service.repository.TopicRepository;
import com.pcc.llm_service.repository.ViewHistoryRepository;
import com.pcc.llm_service.model.ViewHistory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GeminiService {

    private final ContentRepository contentRepository;
    private final SummaryRepository summaryRepository;

    private final TopicRepository topicRepository;
    private final ViewHistoryRepository viewHistoryRepository;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper(); // JSON işlemek için

    public GeminiService(ContentRepository contentRepository, SummaryRepository summaryRepository,
            TopicRepository topicRepository, ViewHistoryRepository viewHistoryRepository) {
        this.contentRepository = contentRepository;
        this.summaryRepository = summaryRepository;
        this.topicRepository = topicRepository;
        this.viewHistoryRepository = viewHistoryRepository;
    }

    /**
     * Topic listesini veritabanından çekip prompt için formatlı string döndürür
     */
    private String getTopicListForPrompt() {
        List<Topic> topics = topicRepository.findAll();
        return topics.stream()
                .map(t -> t.getTopicId() + ". " + t.getName())
                .collect(Collectors.joining(", "));
    }

    // Tetikleyici Fonksiyon
    public void processAllPendingContents() {
        // 1. İşlenmemiş (PENDING) içerikleri bul
        List<Content> pendingContents = contentRepository.findByFetchStatus("PENDING");
        System.out.println("İşlenecek içerik sayısı: " + pendingContents.size());

        for (Content content : pendingContents) {
            try {
                processSingleContent(content);
                System.out.println("12 saniye delay");
                Thread.sleep(12000);
            } catch (Exception e) {
                System.err.println("Hata (" + content.getContentId() + "): " + e.getMessage());
            }
        }
    }

    public void processContentById(java.util.UUID contentId) {
        contentRepository.findById(contentId).ifPresent(this::processSingleContent);
    }

    private void processSingleContent(Content content) {
        // Topic listesini veritabanından çek
        String topicList = getTopicListForPrompt();

        // 2. Gemini için özel Prompt hazırla
        String prompt = "Sen bir haber editörüsün. Aşağıdaki haberi özetle. " +
                "ÖNEMLİ KURALLAR: " +
                "1. Özet yazarken 'Metin şunu anlatıyor', 'Haber şunu ele alıyor', 'Bu içerik...' gibi analitik ifadeler KULLANMA. "
                +
                "2. Doğrudan haberin özünü anlatan, sanki sen haber yazıyormuşsun gibi bir özet yaz. " +
                "3. Özet 2-3 cümle olsun ve doğrudan konuya girsin. " +
                "4. Bu haberin konusunu şu listeden seç: [" + topicList + "]. " +
                "Cevabı SADECE şu JSON formatında ver, başka hiçbir şey yazma: " +
                "{ \"title\": \"Dikkat çekici başlık\", \"summary\": \"Doğrudan özet (analitik dil kullanma)\", \"tags\": \"etiket1, etiket2\", \"topic_id\": 8 } "
                +
                "\n\nHaber metni:\n" + content.getOriginalText();

        // 3. API İsteğini Hazırla (Gemini'nin istediği JSON yapısı)
        // {"contents": [{"parts": [{"text": "prompt..."}]}]}
        String requestBody = "{ \"contents\": [{ \"parts\": [{ \"text\": " + objectMapper.valueToTree(prompt)
                + " }] }] }";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // URL'in sonuna API Key ekleniyor
        String finalUrl = apiUrl + "?key=" + apiKey;

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // 4. İsteği Gönder
        String response = restTemplate.postForObject(finalUrl, entity, String.class);

        // 5. Cevabı İşle ve Kaydet
        saveSummary(content, response);
    }

    private void saveSummary(Content content, String jsonResponse) {
        try {
            // 1. Gemini'nin cevabını temizle ve oku
            JsonNode root = objectMapper.readTree(jsonResponse);
            String aiText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();

            // Kod bloklarını (```json ... ```) temizle
            aiText = aiText.replace("```json", "").replace("```", "").trim();

            // Temizlenen JSON'u Java nesnesine çevir
            JsonNode myJson = objectMapper.readTree(aiText);

            Summary summary = new Summary();
            // Hangi haberin özeti?
            summary.setContent(content);

            // AI'dan gelen verileri bas
            summary.setTitle(myJson.get("title").asText());
            summary.setSummaryText(myJson.get("summary").asText());
            summary.setGeneratedTags(myJson.get("tags").asText());

            // Eğer RSS'ten gelen gerçek bir tarih varsa onu kullan, yoksa şu anı bas.
            if (content.getPublishedDate() != null) {
                summary.setCreatedAt(content.getPublishedDate());
            } else {
                summary.setCreatedAt(java.time.LocalDateTime.now());
            }
            // --------------------------------

            // Topic ID'yi parse et ve summary'e ata
            JsonNode topicIdNode = myJson.get("topic_id");
            if (topicIdNode != null && !topicIdNode.isNull()) {
                int topicId = topicIdNode.asInt();
                topicRepository.findById(topicId).ifPresent(topic -> {
                    summary.setTopic(topic);
                    System.out.println("Topic atandı: " + topicId + " - " + topic.getName());
                });
            }

            summaryRepository.save(summary);

            // Haberin durumunu güncelle
            content.setFetchStatus("PROCESSED");
            contentRepository.save(content);

            System.out.println("Özetlendi: " + summary.getTitle());

        } catch (Exception e) {
            System.err.println("JSON Parse Hatası (" + content.getContentId() + "): " + e.getMessage());
        }
    }

    // Rastgele ve daha önce görülmemiş bir içerik getir, getirirken "Görüldü"
    // olarak işaretle
    // Rastgele ve daha önce görülmemiş bir içerik getir (Opsiyonel Topic ID ile)
    public java.util.Optional<Summary> getAndLogRandomUnseenContent(Long userId, Integer topicId) {
        java.util.Optional<Summary> summaryOpt;

        if (topicId != null) {
            // Belirli bir topic için ara
            summaryOpt = summaryRepository.findRandomUnseenSummaryByTopic(userId, topicId);
        } else {
            // Tamamen rastgele ara
            summaryOpt = summaryRepository.findRandomUnseenSummary(userId);
        }

        if (summaryOpt.isPresent()) {
            Summary summary = summaryOpt.get();
            // Görüldü geçmişine kaydet
            try {
                if (summary.getContent() != null) {
                    ViewHistory history = new ViewHistory();
                    history.setUserId(userId);
                    history.setContentId(summary.getContent().getContentId());
                    viewHistoryRepository.save(history);
                    System.out.println("👁️ İçerik görüldü olarak işaretlendi: User=" + userId + ", Content="
                            + summary.getContent().getContentId() + ", Topic="
                            + (topicId != null ? topicId : "Random"));
                }
            } catch (Exception e) {
                System.err.println("Görüldü geçmişi kaydedilemedi: " + e.getMessage());
            }
        } else {
            System.out.println(
                    "🚫 Kullanıcı " + userId + " için " + (topicId != null ? "Topic " + topicId + " konusunda " : "")
                            + "gösterilecek yeni içerik kalmadı!");
        }

        return summaryOpt;
    }

    // Geriye uyumluluk için overload
    public java.util.Optional<Summary> getAndLogRandomUnseenContent(Long userId) {
        return getAndLogRandomUnseenContent(userId, null);
    }
}