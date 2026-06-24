package com.nhnacademy.springailibrarycore.review.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.springailibrarycore.book.repository.BookRepository;
import com.nhnacademy.springailibrarycore.config.RabbitConfig;
import com.nhnacademy.springailibrarycore.review.dto.ReviewSummaryResponse;
import com.nhnacademy.springailibrarycore.review.domain.ReviewStatus;
import com.nhnacademy.springailibrarycore.review.exception.ReviewServiceException;
import com.nhnacademy.springailibrarycore.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.nhnacademy.springailibrarycore.review.dto.ReviewResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ReviewSseSender reviewSseSender;

    public SseEmitter requestReviewSummary(Long bookId) {
        log.info("[ReviewService] 리뷰 요약 요청 시작 -> bookId: {}", bookId);

        // 도서 존재 여부 유효성 검사
        if (!bookRepository.existsById(bookId)) {
            log.warn("[ReviewService] 존재하지 않는 도서 ID에 대한 요약 요청 차단 -> bookId: {}", bookId);
            throw new ReviewServiceException("존재하지 않는 도서 ID입니다. (ID: " + bookId + ")", HttpStatus.NOT_FOUND);
        }

        // 작성된 리뷰 존재 여부 확인
        if (!reviewRepository.existsByBookId(bookId)) {
            log.info("[ReviewService] 작성된 리뷰가 없으므로 Agent 호출 및 캐싱 없이 즉시 종료 -> bookId: {}", bookId);
            SseEmitter emitter = new SseEmitter(180000L);
            reviewSseSender.addEmitter(bookId, emitter);

            ReviewSummaryResponse emptyResponse = new ReviewSummaryResponse(
                    bookId,
                    ReviewStatus.DONE,
                    "아직 작성된 독자 리뷰가 없습니다.",
                    null,
                    null
            );
            reviewSseSender.sendSummary(bookId, emptyResponse);
            return emitter;
        }

        // 3분 타임아웃을 둔 SSE Emitter 생성 및 등록
        SseEmitter emitter = new SseEmitter(180000L);
        reviewSseSender.addEmitter(bookId, emitter);

        // 캐시 및 신규 리뷰 개수 1차 검증
        ReviewSummaryResponse cachedResponse = getCachedSummary(bookId);

        if (cachedResponse != null) {
            Long lastReviewId = cachedResponse.lastProcessReviewId();
            if (lastReviewId != null) {
                // 큐 발행 전에 신규 리뷰 개수를 먼저 확인!
                long newReviewCount = reviewRepository.countByBookIdAndIdGreaterThan(bookId, lastReviewId);
                log.info("[ReviewService] 기존 캐시 요약 존재. 신규 리뷰 개수: {}개", newReviewCount);

                if (newReviewCount < 10) {
                    // 10개 미만이면 RabbitMQ에 던지지 않고 캐시를 즉시 돌려주고 조기 종료
                    log.info("[ReviewService] 신규 리뷰가 10개 미만이므로 큐 발행 생략. 즉시 캐시 전송 -> bookId: {}", bookId);
                    reviewSseSender.sendSummary(bookId, cachedResponse);
                    return emitter;
                }
            }
        }

        // 캐시가 아예 없거나, 신규 리뷰가 10개 이상 쌓인 경우에만 비동기 큐 발행
        log.info("[ReviewService] 캐시 미스 또는 신규 리뷰 10개 이상({}개) 확인. RabbitMQ 이벤트 발행", bookId);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, RabbitConfig.ROUTING_KEY, bookId);

        return emitter;
    }

    public ReviewSummaryResponse getCachedSummary(Long bookId) {
        String cacheKey = "review:summary:" + bookId;
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);

        if (cachedValue == null || cachedValue.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(cachedValue, ReviewSummaryResponse.class);
        } catch (JsonProcessingException e) {
            log.error("[ReviewService] 캐시 데이터 파싱 실패 -> bookId: {}", bookId, e);
            return null;
        }
    }

    /**
     * 특정 도서의 리뷰 목록을 페이징 처리하여 조회합니다.
     *
     * @param bookId   도서 ID
     * @param pageable 페이징 및 정렬 정보
     * @return 페이징된 ReviewResponse DTO 목록
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByBookId(Long bookId, Pageable pageable) {
        log.info("[ReviewService] 도서 리뷰 페이징 조회 -> bookId: {}, page: {}, size: {}", 
                bookId, pageable.getPageNumber(), pageable.getPageSize());

        if (!bookRepository.existsById(bookId)) {
            throw new ReviewServiceException("존재하지 않는 도서 ID입니다. (ID: " + bookId + ")", HttpStatus.NOT_FOUND);
        }

        return reviewRepository.findByBookId(bookId, pageable)
                .map(ReviewResponse::from);
    }
}

