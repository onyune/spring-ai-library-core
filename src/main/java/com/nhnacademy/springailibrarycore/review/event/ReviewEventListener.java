package com.nhnacademy.springailibrarycore.review.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.springailibrarycore.agent.ReviewAgent;
import com.nhnacademy.springailibrarycore.config.RabbitConfig;
import com.nhnacademy.springailibrarycore.review.domain.ReviewStatus;
import com.nhnacademy.springailibrarycore.review.dto.ReviewSummaryResponse;
import com.nhnacademy.springailibrarycore.review.service.ReviewSseSender;
import com.nhnacademy.springailibrarycore.review.exception.ReviewSummaryGenerationException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewEventListener {
    private final ReviewAgent reviewAgent;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final ReviewSseSender reviewSseSender;

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void receive(Long bookId) throws JsonProcessingException {
        try {
            String cacheKey = "review:summary:" + bookId;
            String strResponse = redisTemplate.opsForValue().get(cacheKey);

            if (strResponse != null) {
                log.info("[ReviewEventListener] Cache hit on listener. Sending existing summary to SSE -> bookId: {}", bookId);
                ReviewSummaryResponse response = objectMapper.readValue(strResponse, ReviewSummaryResponse.class);
                reviewSseSender.sendSummary(bookId, response);
                return;
            }

            log.info("리뷰 요약 시도 -> bookId: {}", bookId);
            String summaryText = reviewAgent.summarizeReviews(bookId);

            ReviewSummaryResponse response = new ReviewSummaryResponse(bookId, ReviewStatus.DONE, summaryText, null);

            String jsonValue = objectMapper.writeValueAsString(response);
            redisTemplate.opsForValue().set(cacheKey, jsonValue, Duration.ofDays(1));
            log.info("[ReviewEventListener] Redis 캐시 저장 완료 (key: {})", cacheKey);

            reviewSseSender.sendSummary(bookId, response);

        } catch (Exception e) {
            log.error("리뷰 요약 실패 bookId: {}", bookId, e);
            try {
                ReviewSummaryResponse errorResponse = new ReviewSummaryResponse(
                        bookId,
                        ReviewStatus.ERROR,
                        null,
                        "AI 리뷰 요약 도중 오류가 발생했습니다: " + e.getMessage()
                );
                reviewSseSender.sendSummary(bookId, errorResponse);
            } catch (Exception sseEx) {
                log.error("[ReviewEventListener] SSE 실패 전송 에러 -> bookId: {}", bookId, sseEx);
            }
            throw new ReviewSummaryGenerationException(bookId, e);
        }
    }
}


