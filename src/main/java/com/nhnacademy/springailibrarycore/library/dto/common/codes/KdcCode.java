package com.nhnacademy.springailibrarycore.library.dto.common.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 한국십진분류법(KDC, Korean Decimal Classification)의 대분류(10대 주제) 분류군 코드입니다.
 */
@Getter
@RequiredArgsConstructor
public enum KdcCode {
    /** 총류 (코드: "0") */
    GENERAL("0", "총류"),
    /** 철학 (코드: "1") */
    PHILOSOPHY("1", "철학"),
    /** 종교 (코드: "2") */
    RELIGION("2", "종교"),
    /** 사회과학 (코드: "3") */
    SOCIAL_SCIENCE("3", "사회과학"),
    /** 자연과학 (코드: "4") */
    NATURAL_SCIENCE("4", "자연과학"),
    /** 기술과학 (코드: "5") */
    TECHNOLOGY_SCIENCE("5", "기술과학"),
    /** 예술 (코드: "6") */
    ARTS("6", "예술"),
    /** 언어 (코드: "7") */
    LANGUAGE("7", "언어"),
    /** 문학 (코드: "8") */
    LITERATURE("8", "문학"),
    /** 역사 (코드: "9") */
    HISTORY("9", "역사");

    private final String value;
    private final String desc;
}
