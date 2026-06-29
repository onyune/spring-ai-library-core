package com.nhnacademy.springailibrarycore.telegram.client;

import com.nhnacademy.springailibrarycore.book.dto.FeedbackLikedBooksResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

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
            // 예외 발생 시 빈 리스트 반환
            return new FeedbackLikedBooksResponse(chatId, List.of());
        }
    }
}
