package com.nhnacademy.springailibrarycore.library.dto.common.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * ISBN 부가기호 코드 종류(독자대상기호)를 정의한 공통 Enum입니다.
 * <p>
 * ISBN 부가기호 5자리 중 첫 번째 자리(독자대상기호)에 해당하는 코드입니다.
 * </p>
 */
@Getter
@RequiredArgsConstructor
public enum AddCode {
    /** 교양 도서 (코드: "0") */
    GENERAL("0", "교양"),
    /** 실용 도서 (코드: "1") */
    PRACTICAL("1", "실용"),
    /** 여성 도서 (코드: "2") */
    WOMEN("2", "여성"),
    /** 청소년 도서 (코드: "4") */
    TEENS("4", "청소년"),
    /** 중고교 학습참고서 (코드: "5") */
    STUDY_REFERENCE_1("5", "학습참고서1(중고)"),
    /** 초등 학습참고서 (코드: "6") */
    STUDY_REFERENCE_2("6", "학습참고서2(초등)"),
    /** 아동 도서 (코드: "7") */
    CHILDREN("7", "아동"),
    /** 전문 도서 (코드: "9") */
    PROFESSIONAL("9", "전문");

    private final String value;
    private final String desc;
}
