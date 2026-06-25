package com.nhnacademy.springailibrarycore.library.dto.request;

import lombok.Builder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 도서 상세 조회 API (srchDtlList) 호출을 위한 요청 DTO 클래스
 * 특정 도서의 기본 서지 정보와 함께 추가적인 빅데이터 대출 통계 분석(성별, 연령별, 지역별 조회 분포 등)을 요청할 수 있습니다.
 *
 * @param isbn13      조회할 도서의 13자리 ISBN 번호 (필수)
 * @param loaninfoYN  추가 대출 통계 정보를 제공할지 여부 ("Y": 통계 정보 포함, "N": 서지 정보만 반환. 기본값: "N")
 * @param displayInfo 통계 정보를 포함할 경우(loaninfoYN=Y), 반환할 인구통계 결과 그룹 필터입니다.
 *                    세미콜론(;)을 구분자로 사용하여 다중 선택이 가능 (예: "gender;age;region")
 *                    gender: 성별 대출 집계 결과
 *                    age: 연령별 대출 집계 결과
 *                    region: 지역별 대출 집계 결과
 */
@Builder
public record BookDetailRequest(
        String isbn13,
        String loaninfoYN,  // Y: 제공, N: 미제공 (기본값 N)
        String displayInfo  // gender: 성별, age: 연령별, region: 지역별 (loaninfoYN=Y 인 경우 선택 필터)
) {
    /**
     * RestClient 요청 시 쿼리 파라미터 맵으로 변환해 주는 유틸 메서드입니다.
     * 
     * @return 쿼리 파라미터 Key-Value 맵
     */
    public MultiValueMap<String, String> toQueryParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        if (isbn13 != null && !isbn13.isBlank()) params.add("isbn13", isbn13);
        if (loaninfoYN != null && !loaninfoYN.isBlank()) params.add("loaninfoYN", loaninfoYN);
        if (displayInfo != null && !displayInfo.isBlank()) params.add("displayInfo", displayInfo);
        return params;
    }
}
