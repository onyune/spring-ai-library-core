package com.nhnacademy.springailibrarycore.book.dto;

/**
 * AI 추천 응답 DTO
 *
 * AI가 생성한 추천 결과를 담습니다.
 */
public record BookAiRecommendationResponse(
        long id,            // 도서 ID
        int relevance,      // 연관성 점수 (0-100)
        String comment          // 추천 사유
) {
}