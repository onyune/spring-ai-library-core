package com.nhnacademy.springailibrarycore.review.dto;

import jakarta.validation.constraints.*;

public record ReviewCreateRequest(
    @NotNull(message = "리뷰 대상 도서 ID는 필수입니다.")
    Long bookId,

    @NotBlank(message = "리뷰 작성자 식별값은 필수입니다.")
    @Size(max = 100, message = "작성자 ID는 100자를 초과할 수 없습니다.")
    String reviewerId,

    @Min(value = 1, message = "평점은 최소 1점 이상이어야 합니다.")
    @Max(value = 5, message = "평점은 최대 5점 이하이어야 합니다.")
    int rating,

    @NotBlank(message = "리뷰 내용은 필수입니다.")
    @Size(max = 2000, message = "리뷰 내용은 2000자를 초과할 수 없습니다.")
    String content
) {}
