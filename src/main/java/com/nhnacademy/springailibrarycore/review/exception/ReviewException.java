package com.nhnacademy.springailibrarycore.review.exception;

import org.springframework.http.HttpStatus;

public abstract class ReviewException extends RuntimeException {
    private final HttpStatus status;

    protected ReviewException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
