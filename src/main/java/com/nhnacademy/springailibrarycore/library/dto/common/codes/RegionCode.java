package com.nhnacademy.springailibrarycore.library.dto.common.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 행정구역 시도 코드(2자리)를 정의한 공통 Enum입니다.
 */
@Getter
@RequiredArgsConstructor
public enum RegionCode {
    /** 서울특별시 (코드: "11") */
    SEOUL("11", "서울특별시"),
    /** 부산광역시 (코드: "21") */
    BUSAN("21", "부산광역시"),
    /** 대구광역시 (코드: "22") */
    DAEGU("22", "대구광역시"),
    /** 인천광역시 (코드: "23") */
    INCHEON("23", "인천광역시"),
    /** 광주광역시 (코드: "24") */
    GWANGJU("24", "광주광역시"),
    /** 대전광역시 (코드: "25") */
    DAEJEON("25", "대전광역시"),
    /** 울산광역시 (코드: "26") */
    ULSAN("26", "울산광역시"),
    /** 세종특별자치시 (코드: "29") */
    SEJONG("29", "세종특별자치시"),
    /** 경기도 (코드: "31") */
    GYEONGGI("31", "경기도"),
    /** 강원특별자치도 (코드: "32") */
    GANGWON("32", "강원특별자치도"),
    /** 충청북도 (코드: "33") */
    CHUNGBUK("33", "충청북도"),
    /** 충청남도 (코드: "34") */
    CHUNGNAM("34", "충청남도"),
    /** 전북특별자치도 (코드: "35") */
    JEONBUK("35", "전북특별자치도"),
    /** 전라남도 (코드: "36") */
    JEONNAM("36", "전라남도"),
    /** 경상북도 (코드: "37") */
    GYEONGBUK("37", "경상북도"),
    /** 경상남도 (코드: "38") */
    GYEONGNAM("38", "경상남도"),
    /** 제주특별자치도 (코드: "39") */
    JEJU("39", "제주특별자치도");

    private final String value;
    private final String desc;
}
