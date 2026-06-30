package com.nhnacademy.springailibrarycore.library.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;

/**
 * 정보공개 도서관 조회 API 응답 DTO 클래스입니다.
 * 
 * @param response API 응답 데이터 본문
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LibrarySearchResponse(
        ResponseData response
) {
    /**
     * API 응답 데이터 레코드입니다.
     * 
     * @param request 요청 파라미터 메타데이터
     * @param libs    도서관 정보 목록이 감싸진 LibWrapper 리스트
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResponseData(
            RequestMetadata request,
            List<LibWrapper> libs
    ) {}

    /**
     * API 요청 결과에 대한 메타데이터 레코드입니다.
     * 
     * @param pageNo    요청/반환된 페이지 번호
     * @param pageSize  한 페이지에 노출된 항목 수
     * @param numFound  전체 조건 만족 검색 건수
     * @param resultNum 현재 페이지에 반환된 실 결과 건수
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RequestMetadata(
            Integer pageNo,
            Integer pageSize,
            Integer numFound,
            Integer resultNum
    ) {}

    /**
     * 도서관 정보 요소를 감싸는 래퍼 레코드입니다.
     * 
     * @param lib 도서관 상세 정보
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LibWrapper(
            LibraryInfo lib
    ) {}

    /**
     * 도서관의 상세 정보 레코드입니다.
     * 
     * @param libCode       도서관의 고유 10자리 코드
     * @param libName       도서관 이름
     * @param address       도서관 도로명/지번 주소
     * @param tel           도서관 대표 연락처
     * @param fax           도서관 FAX 연락처
     * @param latitude      도서관 위치 위도 값
     * @param longitude     도서관 위치 경도 값
     * @param homepage      도서관 홈페이지 URL
     * @param closed        도서관 정기 휴관일 정보
     * @param operatingTime 도서관 상세 운영시간 안내 정보
     * @param bookCount     도서관 보유 단행본 총 도서 수
     */
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LibraryInfo(
            String libCode,
            String libName,
            String address,
            String tel,
            String fax,
            String latitude,
            String longitude,
            String homepage,
            String closed,
            String operatingTime,
            @JsonProperty("BookCount") String bookCount
    ) {}
}
