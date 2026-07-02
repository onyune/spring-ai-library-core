package com.nhnacademy.springailibrarycore.book.dto.ai;

import java.util.List;
import org.springframework.context.annotation.Description;

/**
 * AI가 반환하는 전체 추천 결과.
 *
 * @param recommendations 도서별 추천 정보 목록
 */
public record RecommendationResult(
        @Description("입력된 모든 도서에 대한 추천 정보 목록")
        List<BookRecommendation> recommendations
) {
}