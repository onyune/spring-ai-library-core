package com.nhnacademy.springailibrarycore.library.dto.request;

import lombok.Builder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 신착도서 조회 API (newArrivalBook) 호출을 위한 요청 DTO 클래스
 * 특정 도서관에 최근 등록된 신착 도서 목록을 월 단위로 조회
 *
 * @param libCode  조회 대상 도서관 코드 (필수)
 * @param searchDt 조회할 연월 (형식: "yyyy-MM", 예: "2026-06").
 *                 생략할 경우 조회 당일 기준의 현재 연월이 기본값으로 지정
 */
@Builder
public record NewArrivalBookRequest(
        String libCode,
        String libName,
        String searchDt // yyyy-MM
) {
    public MultiValueMap<String, String> toQueryParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        if (libCode != null && !libCode.isBlank()) params.add("libCode", libCode);
        if (searchDt != null && !searchDt.isBlank()) params.add("searchDt", searchDt);
        return params;
    }
}
