package com.nhnacademy.springailibrarycore.review.controller;

import com.nhnacademy.springailibrarycore.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.nhnacademy.springailibrarycore.review.dto.ReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Slf4j
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 특정 도서의 리뷰 요약 스트림을 구독합니다.
     * 캐시가 있다면 즉시 반환하고 SSE를 종료하며, 없다면 비동기로 RabbitMQ를 통해 처리한 후 SSE로 결과를 내려줍니다.
     *
     * @param bookId 도서 ID
     * @return SseEmitter SSE 이벤트 스트림
     */
    @GetMapping(value = "/summary/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamReviewSummary(@RequestParam("bookId") Long bookId) {
        log.info("[ReviewController] Get review summary stream request for bookId: {}", bookId);
        return reviewService.requestReviewSummary(bookId);
    }

    /**
     * 특정 도서의 리뷰 목록을 페이징하여 조회합니다.
     *
     * @param bookId   도서 ID
     * @param pageable 페이징 정보
     * @return Page<ReviewResponse> 페이징 처리된 리뷰 목록
     */
    @GetMapping
    public ResponseEntity<Page<ReviewResponse>> getReviewsByBookId(
            @RequestParam("bookId") Long bookId,
            Pageable pageable
    ) {
        log.info("[ReviewController] Get paginated reviews request for bookId: {}, page: {}", bookId, pageable.getPageNumber());
        Page<ReviewResponse> response = reviewService.getReviewsByBookId(bookId, pageable);
        return ResponseEntity.ok(response);
    }
}

