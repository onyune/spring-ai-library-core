package com.nhnacademy.springailibrarycore.review.dto;

import com.nhnacademy.springailibrarycore.review.domain.ReviewStatus;

public record ReviewSummaryResponse(
        Long bookId,
        ReviewStatus status,
        String summaryText,
        String errorMessage,
        Long lastProcessReviewId
) {
}
