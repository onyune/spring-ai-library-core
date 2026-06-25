package com.nhnacademy.springailibrarycore.library.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nhnacademy.springailibrarycore.library.dto.common.NaruBookInfo;
import com.nhnacademy.springailibrarycore.library.dto.common.NaruResponse.RequestMetadata;
import com.nhnacademy.springailibrarycore.library.dto.common.NaruWrapper;
import java.util.List;

/**
 * 도서관/지역별 인기대출 도서 조회 API 응답 DTO 클래스입니다.
 * 
 * @param response API 응답 데이터 본문
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LibPopularBooksResponse(
        ResponseData response
) {
    /**
     * API 응답 데이터 레코드입니다.
     * 
     * @param libNm       조회 기준이 도서관일 때 매핑되는 도서관 명칭
     * @param regionNm    조회 기준이 지역일 때 매핑되는 행정구역 시도 명칭
     * @param dtlregionNm 조회 기준이 지역일 때 매핑되는 행정구역 시군구 명칭
     * @param request     요청 파라미터 메타데이터
     * @param docs        도서 정보가 감싸진 NaruWrapper 목록
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResponseData(
            String libNm,
            String regionNm,
            String dtlregionNm,
            RequestMetadata request,
            List<NaruWrapper<NaruBookInfo>> docs
    ) {}
}
