package com.nhnacademy.springailibrarycore.library.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nhnacademy.springailibrarycore.library.dto.common.NaruBookInfo;
import java.util.List;

/**
 * 도서 상세 조회 API 응답 DTO 클래스
 * 
 * @param response API 응답 데이터 본문
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaruBookDetailResponse(
        ResponseData response
) {
    /**
     * API 응답 데이터 레코드
     * 
     * @param detail   상세 서지 정보
     * @param loanInfo 대출 통계 분석 결과 (요청 시 loaninfoYN=Y 인 경우 제공됨)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResponseData(
            Detail detail,
            LoanInfoWrapper loanInfo
    ) {}

    /**
     * 감싸진 상세 서지 데이터 레코드
     * 
     * @param book 도서 상세 정보 객체
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Detail(
            NaruBookInfo book
        ) {}

    /**
     * 대출 집계 통계 데이터를 담고 있는 래퍼 레코드
     * 
     * @param Total        전체 대출 통계 요약 정보
     * @param regionResult 행정구역 지역별 대출 순위 및 건수 정보 목록
     * @param ageResult    연령대별 대출 순위 및 건수 정보 목록
     * @param genderResult 성별 대출 순위 및 건수 정보 목록
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoanInfoWrapper(
            TotalInfo Total,
            List<LoanStatWrapper> regionResult,
            List<LoanStatWrapper> ageResult,
            List<LoanStatWrapper> genderResult
    ) {}

    /**
     * 대출 통계 요약 정보 레코드입니다.
     * 
     * @param ranking 전체 대출 순위
     * @param name    항목 레이블
     * @param loanCnt 대출 건수
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TotalInfo(
            Integer ranking,
            String name,
            Integer loanCnt
    ) {}

    /**
     * 통계 분류별(지역/연령/성별) 감싸진 데이터를 풀어주는 래퍼 레코드
     * 
     * @param region 지역 통계 정보
     * @param age    연령 통계 정보
     * @param gender 성별 통계 정보
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoanStatWrapper(
            LoanStatInfo region,
            LoanStatInfo age,
            LoanStatInfo gender
    ) {
        /**
         * 감싸진 인구통계 결과 개별 정보(Content)를 통합하여 추출해 주는 헬퍼 메서드
         * 
         * @return 현재 매핑된 LoanStatInfo 객체. 없으면 null 반환.
         */
        public LoanStatInfo getContent() {
            if (region != null) return region;
            if (age != null) return age;
            if (gender != null) return gender;
            return null;
        }
    }

    /**
     * 대출 순위 및 건수를 담고 있는 개별 통계 세부 정보 레코드
     * 
     * @param ranking 집계 랭킹
     * @param name    구분 명칭 (예: "20대", "여성", "서울")
     * @param loanCnt 대출 횟수
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoanStatInfo(
            Integer ranking,
            String name,
            Integer loanCnt
    ) {}
}
