package com.nhnacademy.springailibrarycore.review.exception;

import org.springframework.http.HttpStatus;

public class ReviewNotFoundException extends ReviewException {
    public ReviewNotFoundException(Long reviewId) {
        super("리뷰를 찾을 수 없습니다. (ID: " + reviewId + ")", HttpStatus.NOT_FOUND);
    }
}
