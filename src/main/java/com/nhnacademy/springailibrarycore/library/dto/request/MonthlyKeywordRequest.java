package com.nhnacademy.springailibrarycore.library.dto.request;

import lombok.Builder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 이달의 키워드 조회 API (monthlyKeywords) 호출을 위한 요청 DTO 클래스
 * <p>
 * 특정 연월의 대출 데이터에서 명사를 추출하여 TF-IDF 가중치를 적용한 이달의 키워드 데이터를 조회
 * </p>
 *
 * @param month 조회할 연월 (형식: "yyyy-MM", 예: "2026-05").
 *              생략 시 도서관 정보나루 빅데이터 플랫폼에서 가장 최근 집계 완료된 직전 월의 데이터를 기본값으로 반환
 */
@Builder
public record MonthlyKeywordRequest(
        String month // yyyy-MM 형식
) {
    public MultiValueMap<String, String> toQueryParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        if (month != null && !month.isBlank()) params.add("month", month);
        return params;
    }
}
