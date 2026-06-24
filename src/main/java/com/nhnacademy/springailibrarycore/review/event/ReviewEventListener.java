package com.nhnacademy.springailibrarycore.review.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nhnacademy.springailibrarycore.agent.ReviewCoordinator;
import com.nhnacademy.springailibrarycore.config.RabbitConfig;
import com.nhnacademy.springailibrarycore.review.domain.ReviewStatus;
import com.nhnacademy.springailibrarycore.review.dto.ReviewSummaryResponse;
import com.nhnacademy.springailibrarycore.review.service.ReviewSseSender;
import com.nhnacademy.springailibrarycore.review.exception.ReviewSummaryGenerationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewEventListener {
    private final ReviewCoordinator reviewCoordinator;
    private final ReviewSseSender reviewSseSender;

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void receive(Long bookId) throws JsonProcessingException {
        try {
            ReviewSummaryResponse response = reviewCoordinator.getOrGenerateSummary(bookId);
            reviewSseSender.sendSummary(bookId, response);

        } catch (Exception e) {
            log.error("리뷰 요약 실패 bookId: {}", bookId, e);
            try {
                ReviewSummaryResponse errorResponse = new ReviewSummaryResponse(
                        bookId,
                        ReviewStatus.ERROR,
                        null,
                        "AI 리뷰 요약 도중 오류가 발생했습니다: " + e.getMessage(),
                        null
                );
                reviewSseSender.sendSummary(bookId, errorResponse);
            } catch (Exception sseEx) {
                log.error("[ReviewEventListener] SSE 실패 전송 에러 -> bookId: {}", bookId, sseEx);
            }
            throw new ReviewSummaryGenerationException(bookId, e);
        }
    }
}


