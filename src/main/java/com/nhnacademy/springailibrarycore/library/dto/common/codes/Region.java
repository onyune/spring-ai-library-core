package com.nhnacademy.springailibrarycore.library.dto.common.codes;

import java.util.Arrays;

public enum Region {
    SEOUL("11", "서울특별시", "서울"),
    SEJONG("29", "세종특별자치시", "세종"),
    BUSAN("21", "부산광역시", "부산"),
    GYEONGGI("31", "경기도", "경기"),
    DAEGU("22", "대구광역시", "대구"),
    GANGWON("32", "강원특별자치도", "강원"),
    INCHEON("23", "인천광역시", "인천"),
    CHUNGBUK("33", "충청북도", "충북"),
    GWANGJU("24", "광주광역시", "광주"),
    CHUNGNAM("34", "충청남도", "충남"),
    DAEJEON("25", "대전광역시", "대전"),
    JEONBUK("35", "전북특별자치도", "전북"),
    ULSAN("26", "울산광역시", "울산"),
    JEONNAM("36", "전라남도", "전남"),
    GYEONGBUK("37", "경상북도", "경북"),
    GYEONGNAM("38", "경상남도", "경남"),
    JEJU("39", "제주특별자치도", "제주");

    private final String code;
    private final String fullName;
    private final String shortName;

    Region(String code, String fullName, String shortName) {
        this.code = code;
        this.fullName = fullName;
        this.shortName = shortName;
    }

    public String getCode() {
        return code;
    }

    public String getFullName() {
        return fullName;
    }

    public String getShortName() {
        return shortName;
    }

    public static Region fromName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String cleanName = name.replaceAll("\\s+", "");
        return Arrays.stream(values())
                .filter(r -> cleanName.contains(r.fullName) || cleanName.contains(r.shortName))
                .findFirst()
                .orElse(null);
    }
    
    public static Region fromCode(String code) {
        return Arrays.stream(values())
                .filter(r -> r.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}
