package com.nhnacademy.springailibrarycore.library.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 이달의 키워드 조회 API 응답 DTO 클래스
 * 
 * @param response API 응답 데이터 본문
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MonthlyKeywordResponse(
        ResponseData response
) {
    /**
     * API 응답 데이터 레코드
     * 
     * @param keywords 감싸진 키워드 데이터 목록
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResponseData(
            List<KeywordWrapper> keywords
    ) {}

    /**
     * 키워드 요소를 감싸는 래퍼 레코드
     * 
     * @param keyword 키워드 및 가중치 상세 정보
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KeywordWrapper(
            KeywordInfo keyword
    ) {}

    /**
     * 키워드 세부 정보 레코드
     * 
     * @param word   키워드 단어명
     * @param weight TF-IDF 등으로 계산된 해당 단어의 가중치 점수
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KeywordInfo(
            String word,
            Double weight
    ) {}
}
