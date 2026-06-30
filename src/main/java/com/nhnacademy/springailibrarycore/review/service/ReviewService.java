package com.nhnacademy.springailibrarycore.review.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.springailibrarycore.book.domain.Book;
import com.nhnacademy.springailibrarycore.book.repository.BookRepository;
import com.nhnacademy.springailibrarycore.config.RabbitConfig;
import com.nhnacademy.springailibrarycore.review.domain.Review;
import com.nhnacademy.springailibrarycore.review.dto.ReviewCreateRequest;
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

import java.util.UUID;

/**
 *
 */
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

        // 작성된 전체 리뷰 개수 확인 및 10개 미만 차단
        long totalReviewCount = reviewRepository.countByBookId(bookId);
        if (totalReviewCount < 10) {
            log.info("[ReviewService] 전체 리뷰 개수가 10개 미만({}개)이므로 요약 생략 -> bookId: {}", totalReviewCount, bookId);
            SseEmitter emitter = new SseEmitter(180000L);
            reviewSseSender.addEmitter(bookId, emitter);

            // 0개인 경우와 1~9개인 경우의 반환 메시지 세분화
            String message = (totalReviewCount == 0) 
                    ? "아직 작성된 독자 리뷰가 없습니다." 
                    : String.format("리뷰가 최소 10개 이상 작성되어야 AI 리뷰 요약이 가능합니다. (현재 리뷰: %d개)", totalReviewCount);

            ReviewSummaryResponse emptyResponse = new ReviewSummaryResponse(
                    bookId,
                    ReviewStatus.DONE,
                    message,
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
                // 큐 발행 전에 신규 리뷰 개수를 먼저 확인
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
        log.info("[ReviewService] 캐시 미스 또는 신규 리뷰 10개 이상 확인. RabbitMQ 이벤트 발행 -> bookId: {}", bookId);
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


    /**
     * 리뷰 등록 서비스 한 책당 한 명의 리뷰 작성 가능
     *
     * 로그인 기능이 없으므로 임시 reviewerId를 UUID로 생성한다.
     * 현재 구조에서는 요청마다 새로운 reviewerId가 생성된다.
     * 추후 로그인/회원 기능이 붙으면 request 또는 인증 정보에서 reviewerId를 받아 저장한다.
     */
    @Transactional
    public Long createReview(Long bookId, ReviewCreateRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ReviewServiceException("존재하지 않는 도서 ID입니다. (ID: " + bookId + ")", HttpStatus.NOT_FOUND));

        String reviewerId = "guest-" + UUID.randomUUID();

        if(reviewRepository.existsByBookIdAndReviewerId(bookId,reviewerId)) {
            throw new ReviewServiceException("이미 이 도서에 리뷰를 작성했습니다. reviewerId=" + reviewerId + ", bookId=" + bookId, HttpStatus.CONFLICT);
        }

        Review review = new Review(
                book,
                reviewerId,
                request.rating(),
                request.content()

        );


        Review savedReview = reviewRepository.save(review);


        return savedReview.getId();
    }
}

