package com.nhnacademy.springailibrarycore.book.service;


import com.nhnacademy.springailibrarycore.book.dto.BookFeedbackStatistics;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class FeedbackInternalService {
    private final RestClient restClient;

    public FeedbackInternalService(RestClient.Builder restClientBuilder, @Value("${feedback.service.url}") String feedbackServiceUrl) {
        this.restClient = restClientBuilder
                .baseUrl(feedbackServiceUrl)
                .build();
    }

    public Map<Long, BookFeedbackStatistics> getBooksFeedbackStats(List<Long> bookIds) {
        if(bookIds == null || bookIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            log.info("[FeedbackService] 도서 {}권에 대한 피드백 통계 조회 요청", bookIds.size());

            List<BookFeedbackStatistics> statsList = restClient.post()
                    .uri("/api/feedback/stats")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(bookIds)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<BookFeedbackStatistics>>() {});

            if(statsList == null || statsList.isEmpty()) {
                return Collections.emptyMap();
            }

            return statsList.stream()
                    .collect(Collectors.toMap(
                            BookFeedbackStatistics::bookId,
                            Function.identity(),
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.error("[FeedbackRestClient] 피드백 통계 HTTP 통신 실패 (Fallback 빈 Map 반환)", e);
            return Collections.emptyMap();
        }
    }

}
