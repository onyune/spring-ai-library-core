package com.nhnacademy.springailibrarycore.review.exception;

import org.springframework.http.HttpStatus;

public class ReviewServiceException extends ReviewException {
    public ReviewServiceException(String message, HttpStatus status) {
        super(message, status);
    }
}
