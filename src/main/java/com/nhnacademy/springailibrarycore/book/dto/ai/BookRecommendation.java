package com.nhnacademy.springailibrarycore.book.dto.ai;

import com.fasterxml.jackson.annotation.JsonAlias;
import org.springframework.context.annotation.Description;

/**
 * AI가 반환하는 도서 1권의 추천 정보.
 *
 * @param bookId    입력으로 받은 bookId (매핑용)
 * @param relevance 질문과의 연관성 점수 (0~100)
 * @param aiComment 추천 사유 (한국어 2~3문장)
 */
public record BookRecommendation(
        @Description("입력으로 받은 bookId 그대로")
        Long bookId,
        @Description("질문과의 연관성 점수 0~100 정수")
        @JsonAlias({"score", "연관도"})
        Integer relevance,
        @Description("이 도서를 추천하는 이유, 한국어 2~3문장")
        @JsonAlias({"answer", "comment", "추천사유"})
        String aiComment
) {
}