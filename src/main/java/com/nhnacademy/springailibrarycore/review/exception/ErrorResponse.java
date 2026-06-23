package com.nhnacademy.springailibrarycore.review.exception;

public record ErrorResponse(
        int status,
        String message
) {
}
