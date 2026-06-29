package com.nhnacademy.springailibrarycore.review.dto;

import jakarta.validation.constraints.*;

public record ReviewCreateRequest(
    @Min(value = 1, message = "평점은 최소 1점 이상이어야 합니다.")
    @Max(value = 5, message = "평점은 최대 5점 이하이어야 합니다.")
    int rating,

    @NotBlank(message = "리뷰 내용은 필수입니다.")
    @Size(max = 2000, message = "리뷰 내용은 2000자를 초과할 수 없습니다.")
    String content
) {}
