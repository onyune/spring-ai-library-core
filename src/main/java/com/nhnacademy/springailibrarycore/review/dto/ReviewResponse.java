package com.nhnacademy.springailibrarycore.review.dto;

import com.nhnacademy.springailibrarycore.review.domain.Review;
import java.time.OffsetDateTime;

public record ReviewResponse(
        Long id,
        String reviewerId,
        int rating,
        String content,
        OffsetDateTime createdAt
) {
    public static ReviewResponse from(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getReviewerId(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt()
        );
    }
}
