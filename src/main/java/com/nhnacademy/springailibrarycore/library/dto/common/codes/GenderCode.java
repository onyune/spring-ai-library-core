package com.nhnacademy.springailibrarycore.library.dto.common.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 도서 대출 통계 분석 시 사용하는 성별 구분 코드입니다.
 */
@Getter
@RequiredArgsConstructor
public enum GenderCode {
    /** 남성 (코드: "0") */
    MALE("0", "남성"),
    /** 여성 (코드: "1") */
    FEMALE("1", "여성"),
    /** 미상/기타 (코드: "2") */
    UNKNOWN("2", "미상");

    private final String value;
    private final String desc;
}
