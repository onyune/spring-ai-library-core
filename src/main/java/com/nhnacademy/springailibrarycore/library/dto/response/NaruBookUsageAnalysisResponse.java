package com.nhnacademy.springailibrarycore.library.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.nhnacademy.springailibrarycore.library.dto.common.NaruBookInfo;
import java.util.List;

/**
 * 도서별 이용 분석 및 연계 분석 데이터 조회 API 응답 DTO 클래스
 * 
 * @param response API 응답 데이터 본문
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaruBookUsageAnalysisResponse(
        ResponseData response
) {
    /**
     * API 응답 데이터 레코드입니다.
     * 
     * @param book           대상 도서의 기본 정보
     * @param loanHistory    최근 12개월간의 월별 대출 통계 이력
     * @param loanGrps       최근 30일간 해당 도서를 가장 많이 빌린 주 이용층(성별/연령) 목록
     * @param keywords       도서 키워드 단어 및 가중치 목록
     * @param coLoanBooks    해당 도서와 동시 대출 빈도가 높은 도서 목록 (최대 10권)
     * @param maniaRecBooks  해당 도서에 대한 마니아 조건부 확률 추천 도서 목록 (최대 10권)
     * @param readerRecBooks 해당 도서에 대한 다독자 조건부 확률 추천 도서 목록 (최대 10권)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResponseData(
            NaruBookInfo book,
            List<LoanHistoryWrapper> loanHistory,
            List<LoanGrpWrapper> loanGrps,
            List<KeywordWrapper> keywords,
            List<BookWrapper> coLoanBooks,
            List<BookWrapper> maniaRecBooks,
            List<BookWrapper> readerRecBooks
    ) {}

    /**
     * 대출 월별 이력을 감싸는 래퍼 레코드입니다.
     * 
     * @param loan 대출 월별 정보
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoanHistoryWrapper(
            LoanHistoryInfo loan
    ) {}

    /**
     * 월별 대출 정보 레코드입니다.
     * 
     * @param month   통계 대상 연월 (yyyy-MM)
     * @param loanCnt 대출 건수
     * @param ranking 전국 대출 순위
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoanHistoryInfo(
            String month,
            Integer loanCnt,
            Integer ranking
    ) {}

    /**
     * 이용 그룹 통계를 감싸는 래퍼 레코드입니다.
     * 
     * @param loanGrp 이용 그룹 상세 정보
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoanGrpWrapper(
            LoanGrpInfo loanGrp
    ) {}

    /**
     * 대출 주 이용자 그룹에 대한 정보 레코드입니다.
     * 
     * @param age     연령대 구분 명칭 (예: "40대")
     * @param gender  성별 명칭 (예: "여성")
     * @param loanCnt 대출 건수
     * @param ranking 해당 그룹 내 순위
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LoanGrpInfo(
            String age,
            String gender,
            Integer loanCnt,
            Integer ranking
    ) {}

    /**
     * 핵심 키워드 단어를 감싸는 래퍼 레코드입니다.
     * 
     * @param keyword 키워드 정보
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KeywordWrapper(
            KeywordInfo keyword
    ) {}

    /**
     * 형태소 분석 추출 핵심 키워드 정보 레코드입니다.
     * 
     * @param word   키워드 단어
     * @param weight 가중치 점수
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KeywordInfo(
            String word,
            Double weight
    ) {}

    /**
     * 추천/동시대출 도서 리스트를 감싸는 래퍼 레코드입니다.
     * 
     * @param book 추천 대상 도서 정보
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BookWrapper(
            NaruBookInfo book
    ) {}
}
