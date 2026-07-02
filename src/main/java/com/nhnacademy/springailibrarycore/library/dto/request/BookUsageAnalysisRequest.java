package com.nhnacademy.springailibrarycore.library.dto.request;

import lombok.Builder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 특정 도서의 이용 분석 및 연계 분석 데이터 조회 API (usageAnalysisList) 호출을 위한 요청 DTO 클래스
 * 대상 도서의 최근 월별 대출 추이, 주 대출 이용 그룹 정보, 형태소 분석 핵심 키워드, 동시 대출 도서, 마니아/다독자 조건부 확률 추천 목록을 조회합니다.
 *
 * @param isbn13 분석 대상 도서의 13자리 ISBN 번호 (필수)
 */
@Builder
public record BookUsageAnalysisRequest(
        String isbn13
) {
    public MultiValueMap<String, String> toQueryParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        if (isbn13 != null && !isbn13.isBlank()) params.add("isbn13", isbn13);
        return params;
    }
}
