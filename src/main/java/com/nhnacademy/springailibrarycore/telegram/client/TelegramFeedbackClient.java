package com.nhnacademy.springailibrarycore.telegram.client;

import com.nhnacademy.springailibrarycore.book.dto.FeedbackLikedBooksResponse;
import com.nhnacademy.springailibrarycore.book.dto.FeedbackStats;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class TelegramFeedbackClient {

    private final RestClient restClient;
    private final String telegramRepositoryUrl;

    public TelegramFeedbackClient(RestClient.Builder restClientBuilder,
                                  @Value("${telegram.repository.url:http://localhost:8081}") String telegramRepositoryUrl) {
        this.restClient = restClientBuilder.build();
        this.telegramRepositoryUrl = telegramRepositoryUrl;
    }

    public FeedbackLikedBooksResponse getLikedBooks(Long chatId) {
        String url = UriComponentsBuilder.fromUriString(telegramRepositoryUrl + "/api/telegram/feedback/{chatId}/liked-books")
                .queryParam("limit", 20)
                .buildAndExpand(chatId)
                .toUriString();

        try {
            return restClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .body(FeedbackLikedBooksResponse.class);
        } catch (Exception e) {
            log.error("Failed to fetch liked books for chatId: {}", chatId, e);
            return new FeedbackLikedBooksResponse(chatId, List.of());
        }
    }

    public Map<Long, FeedbackStats> getBooksFeedbackStats(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            log.info("[FeedbackService] 도서 {}권에 대한 피드백 통계 Map 직접 조회 요청", bookIds.size());

            URI uri = UriComponentsBuilder
                    .fromUriString(telegramRepositoryUrl + "/api/admin/feedback/books/stats")
                    .queryParam("bookIds", bookIds)
                    .build()
                    .toUri();

            Map<Long, FeedbackStats> statsMap = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<Long, FeedbackStats>>() {});
            if (statsMap == null) {
                return Collections.emptyMap();
            }

            return statsMap;
        } catch (Exception e) {
            log.error("[FeedbackRestClient] 피드백 통계 HTTP 통신 실패 (Fallback 빈 Map 반환)", e);
            return Collections.emptyMap();
        }
    }
}