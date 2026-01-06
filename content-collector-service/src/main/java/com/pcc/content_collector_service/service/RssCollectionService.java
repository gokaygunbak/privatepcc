package com.pcc.content_collector_service.service;

import com.pcc.content_collector_service.model.Content;
import com.pcc.content_collector_service.model.Source;
import com.pcc.content_collector_service.repository.ContentRepository;
import com.pcc.content_collector_service.repository.SourceRepository;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class RssCollectionService {

    private final SourceRepository sourceRepository;
    private final ContentRepository contentRepository;

    // Türkçe karakter dönüşüm tablosu
    private static final String TURKISH_CHARS = "çğıöşüÇĞİÖŞÜ";
    private static final String ENGLISH_CHARS = "cgiosuCGIOSU";

    public RssCollectionService(SourceRepository sourceRepository, ContentRepository contentRepository) {
        this.sourceRepository = sourceRepository;
        this.contentRepository = contentRepository;
    }

    // Tüm kaynakları gezip veri toplama emri
    public void collectAllContent() {
        List<Source> sources = sourceRepository.findAll();

        for (Source source : sources) {
            System.out.println("Taranıyor: " + source.getName());
            fetchFromSource(source);
        }
    }

    // Tek bir kaynaktan veri çekme (ROME kütüphanesi)
    private void fetchFromSource(Source source) {
        try {
            java.net.HttpURLConnection httpcon = (java.net.HttpURLConnection) new URL(source.getUrl()).openConnection();
            httpcon.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            httpcon.setConnectTimeout(10000);
            httpcon.setReadTimeout(10000);

            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(httpcon));

            for (SyndEntry entry : feed.getEntries()) {
                if (contentRepository.existsByOriginalUrl(entry.getLink())) {
                    continue;
                }

                Content content = new Content();
                content.setSourceId(source.getSourceId());
                content.setOriginalTitle(entry.getTitle());
                content.setOriginalUrl(entry.getLink());

                if (entry.getDescription() != null) {
                    content.setOriginalText(entry.getDescription().getValue());
                } else {
                    content.setOriginalText("İçerik çekilemedi.");
                }

                if (entry.getPublishedDate() != null) {
                    content.setPublishedDate(entry.getPublishedDate().toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime());
                } else {
                    content.setPublishedDate(LocalDateTime.now());
                }

                content.setFetchStatus("PENDING");
                contentRepository.save(content);
                System.out.println("Kaydedildi: " + entry.getTitle());
            }

        } catch (Exception e) {
            System.err.println("Hata oluştu (" + source.getName() + "): " + e.getMessage());
        }
    }


    public List<Content> searchFromGoogleNews(String keyword, int limit) {
        List<Content> results = new ArrayList<>();
        
        try {
            // Google News RSS URL'i oluştur
            String encodedKeyword = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String googleNewsUrl = String.format(
                "https://news.google.com/rss/search?q=%s&hl=tr&gl=TR&ceid=TR:tr",
                encodedKeyword
            );
            
            System.out.println(" Google News'ten aranıyor: " + keyword);
            System.out.println("URL: " + googleNewsUrl);

            java.net.HttpURLConnection httpcon = (java.net.HttpURLConnection) new URL(googleNewsUrl).openConnection();
            httpcon.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            httpcon.setConnectTimeout(15000);
            httpcon.setReadTimeout(15000);

            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(httpcon));

            int savedCount = 0;
            for (SyndEntry entry : feed.getEntries()) {
                if (savedCount >= limit) break;

                // URL kontrolü - zaten var mı?
                if (contentRepository.existsByOriginalUrl(entry.getLink())) {
                    // Varsa DB'den çek ve listeye ekle
                    continue;
                }

                Content content = new Content();
                content.setSourceId(null); // Google News kaynağı
                content.setOriginalTitle(cleanGoogleNewsTitle(entry.getTitle()));
                content.setOriginalUrl(entry.getLink());

                if (entry.getDescription() != null) {
                    content.setOriginalText(cleanHtmlTags(entry.getDescription().getValue()));
                } else {
                    content.setOriginalText("İçerik çekilemedi.");
                }

                if (entry.getPublishedDate() != null) {
                    content.setPublishedDate(entry.getPublishedDate().toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime());
                } else {
                    content.setPublishedDate(LocalDateTime.now());
                }

                content.setFetchStatus("PENDING");
                contentRepository.save(content);
                results.add(content);
                savedCount++;
                
                System.out.println("Google News'ten kaydedildi: " + content.getOriginalTitle());
            }

            System.out.println("Toplam " + savedCount + " haber bulundu ve kaydedildi.");

        } catch (Exception e) {
            System.err.println("Google News Hatası: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }


    private String cleanGoogleNewsTitle(String title) {
        if (title == null) return "";
        // Son " - KaynakAdı" kısmını kaldır
        int lastDash = title.lastIndexOf(" - ");
        if (lastDash > 0 && lastDash > title.length() / 2) {
            return title.substring(0, lastDash).trim();
        }
        return title.trim();
    }


    private String cleanHtmlTags(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "").replaceAll("&nbsp;", " ").trim();
    }

    private String normalizeTurkish(String text) {
        if (text == null) return "";
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            int index = TURKISH_CHARS.indexOf(c);
            if (index >= 0) {
                result.append(ENGLISH_CHARS.charAt(index));
            } else {
                result.append(c);
            }
        }
        return result.toString().toLowerCase(Locale.ENGLISH);
    }

    public List<Content> collectAndSaveByKeyword(String keyword, int limit) {
        List<Content> allResults = new ArrayList<>();
        int remainingLimit = limit;

        // ÖNCE: Google News'ten dinamik arama (en güncel haberler)

        System.out.println("Arama başlatılıyor: \"" + keyword + "\"");
        
        List<Content> googleResults = searchFromGoogleNews(keyword, remainingLimit);
        allResults.addAll(googleResults);
        remainingLimit -= googleResults.size();

        // 2️⃣ SONRA: Mevcut RSS kaynaklarından ara (eğer hala limit varsa)
        if (remainingLimit > 0) {
            System.out.println("\nKayıtlı RSS kaynaklarından aranıyor...");
            List<Content> rssResults = searchFromRegisteredSources(keyword, remainingLimit);
            allResults.addAll(rssResults);
        }

        System.out.println("Toplam sonuç: " + allResults.size() + " haber");

        return allResults;
    }

    private List<Content> searchFromRegisteredSources(String keyword, int limit) {
        List<Content> results = new ArrayList<>();
        List<Source> sources = sourceRepository.findAll();
        int savedCount = 0;
        
        String normalizedKeyword = normalizeTurkish(keyword);

        for (Source source : sources) {
            if (savedCount >= limit) break;

            try {
                java.net.HttpURLConnection httpcon = (java.net.HttpURLConnection) new URL(source.getUrl())
                        .openConnection();
                httpcon.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
                httpcon.setConnectTimeout(10000);
                httpcon.setReadTimeout(10000);
                
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new XmlReader(httpcon));

                for (SyndEntry entry : feed.getEntries()) {
                    if (savedCount >= limit) break;

                    String title = entry.getTitle() != null ? entry.getTitle() : "";
                    String desc = entry.getDescription() != null ? entry.getDescription().getValue() : "";

                    // Türkçe karakter destekli arama
                    String normalizedTitle = normalizeTurkish(title);
                    String normalizedDesc = normalizeTurkish(desc);

                    boolean matches = normalizedTitle.contains(normalizedKeyword) ||
                                    normalizedDesc.contains(normalizedKeyword);

                    if (matches) {
                        if (contentRepository.existsByOriginalUrl(entry.getLink())) {
                            continue;
                        }

                        Content content = new Content();
                        content.setSourceId(source.getSourceId());
                        content.setOriginalTitle(title);
                        content.setOriginalUrl(entry.getLink());
                        content.setOriginalText(desc.isEmpty() ? "İçerik çekilemedi." : cleanHtmlTags(desc));

                        if (entry.getPublishedDate() != null) {
                            content.setPublishedDate(entry.getPublishedDate().toInstant()
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDateTime());
                        } else {
                            content.setPublishedDate(LocalDateTime.now());
                        }

                        content.setFetchStatus("PENDING");
                        contentRepository.save(content);
                        results.add(content);
                        savedCount++;
                        
                        System.out.println(source.getName() + "'den kaydedildi: " + title);
                    }
                }
            } catch (Exception e) {
                System.err.println("Kaynak hatası (" + source.getName() + "): " + e.getMessage());
            }
        }
        
        return results;
    }
}