package com.nhnacademy.springailibrarycore.review.exception;

import org.springframework.http.HttpStatus;

public class ReviewSummaryGenerationException extends ReviewException {
    public ReviewSummaryGenerationException(Long bookId, Throwable cause) {
        super("도서 ID " + bookId + "에 대한 AI 리뷰 요약 생성에 실패했습니다. (원인: " + cause.getMessage() + ")", HttpStatus.BAD_GATEWAY);
    }

    public ReviewSummaryGenerationException(String message) {
        super(message, HttpStatus.BAD_GATEWAY);
    }
}
