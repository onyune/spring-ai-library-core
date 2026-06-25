package com.nhnacademy.springailibrarycore.library.dto.common.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 도서 대출 통계 분석 시 사용하는 연령대 구분 코드입니다.
 */
@Getter
@RequiredArgsConstructor
public enum AgeCode {
    /** 영유아 (0~5세) (코드: "0") */
    INFANTS("0", "영유아(0~5세)"),
    /** 유아 (6~7세) (코드: "6") */
    TODDLERS("6", "유아(6~7세)"),
    /** 초등 (8~13세) (코드: "8") */
    ELEMENTARY("8", "초등(8~13세)"),
    /** 청소년 (14~19세) (코드: "14") */
    TEENS("14", "청소년(14~19세)"),
    /** 20대 (코드: "20") */
    TWENTIES("20", "20대"),
    /** 30대 (코드: "30") */
    THIRTIES("30", "30대"),
    /** 40대 (코드: "40") */
    FORTIES("40", "40대"),
    /** 50대 (코드: "50") */
    FIFTIES("50", "50대"),
    /** 60세 이상 (코드: "60") */
    SIXTIES_PLUS("60", "60세 이상"),
    /** 미상/분류되지 않음 (코드: "-1") */
    UNKNOWN("-1", "미상"),
    
    // 상세 학교 급별 분류군
    /** 초등학교 1,2학년 (8~9세) (코드: "a8") */
    ELEM_1_2("a8", "1,2학년(8~9세)"),
    /** 초등학교 3,4학년 (10~11세) (코드: "a10") */
    ELEM_3_4("a10", "3,4학년(10~11세)"),
    /** 초등학교 5,6학년 (12~13세) (코드: "a12") */
    ELEM_5_6("a12", "5,6학년(12~13세)"),
    /** 중학생 (14~16세) (코드: "a14") */
    MIDDLE("a14", "중등(14~16세)"),
    /** 고등학생 (17~19세) (코드: "a17") */
    HIGH("a17", "고등(17~19세)");

    private final String value;
    private final String desc;
}
