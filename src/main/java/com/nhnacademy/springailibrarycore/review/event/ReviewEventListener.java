package com.nhnacademy.springailibrarycore.review.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nhnacademy.springailibrarycore.config.RabbitConfig;
import com.nhnacademy.springailibrarycore.review.domain.ReviewStatus;
import com.nhnacademy.springailibrarycore.review.dto.ReviewSummaryResponse;
import com.nhnacademy.springailibrarycore.review.exception.ReviewSummaryGenerationException;
import com.nhnacademy.springailibrarycore.agent.ReviewCoordinator;
import com.nhnacademy.springailibrarycore.review.service.ReviewSseSender;
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

    /**
     * RabbitMQ 큐에서 요약 이벤트를 수신하여 코디네이터로 처리를 넘기고 대기 중인 SSE 세션으로 결과를 푸시합니다.
     */
    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void receive(Long bookId) throws JsonProcessingException {
        log.info("[ReviewEventListener] RabbitMQ 요약 이벤트 수신 -> bookId: {}", bookId);

        try {
            // 코디네이터를 통해 동기적으로 최종 요약 결과 획득 (캐시 혹은 신규생성)
            ReviewSummaryResponse response = reviewCoordinator.getOrGenerateSummary(bookId);

            // 비동기 리스너 단에서 SSE 클라이언트 세션으로 이벤트 푸시 발송
            log.info("[ReviewEventListener] 비동기 요약 완료. SSE 푸시 전송 -> bookId: {}", bookId);
            reviewSseSender.sendSummary(bookId, response);

        } catch (Exception e) {
            log.error("[ReviewEventListener] 비동기 요약 처리 실패 -> bookId: {}", bookId, e);

            // 실패 이벤트를 SSE로 통보하여 화면 무한 로딩 방지 및 소켓 해제
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
                log.error("[ReviewEventListener] SSE 에러 알림 전송 실패 -> bookId: {}", bookId, sseEx);
            }

            // RabbitMQ가 감지해 재시도 및 DLQ로 이관할 수 있게 예외 발생
            throw new ReviewSummaryGenerationException(bookId, e);
        }
    }
}
