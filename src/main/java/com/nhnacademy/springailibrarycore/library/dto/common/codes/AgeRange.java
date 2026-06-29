package com.nhnacademy.springailibrarycore.library.dto.common.codes;

import java.util.Arrays;

public enum AgeRange {
    INFANT(0, "영유아", new String[]{"영유아", "아기", "영아"}),
    CHILD(6, "유아", new String[]{"유아"}),
    ELEMENTARY(8, "초등", new String[]{"초등", "초등학생", "어린이", "초등학교"}),
    TWENTIES(20, "20대", new String[]{"20대", "대학생", "청년"}),
    TEENAGER(14, "청소년", new String[]{"청소년", "중고생", "고등학생", "중학생", "10대", "학생"}),
    THIRTIES(30, "30대", new String[]{"30대", "직장인"}),
    FORTIES(40, "40대", new String[]{"40대"}),
    FIFTIES(50, "50대", new String[]{"50대"}),
    SIXTIES_OVER(60, "60대 이상", new String[]{"60대", "노인", "어르신", "실버", "70대", "80대", "시니어"});

    private final int code;
    private final String name;
    private final String[] keywords;

    AgeRange(int code, String name, String[] keywords) {
        this.code = code;
        this.name = name;
        this.keywords = keywords;
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String[] getKeywords() {
        return keywords;
    }

    public static AgeRange fromName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String cleanName = name.replaceAll("\\s+", "").toLowerCase();
        
        // 1단계: 이름 또는 코드 번호와 완벽히 일치하는 경우 우선 매핑
        for (AgeRange age : values()) {
            if (cleanName.equals(age.name) || cleanName.equals(String.valueOf(age.code))) {
                return age;
            }
        }
        
        // 2단계: 키워드 리스트 매칭
        for (AgeRange age : values()) {
            for (String kw : age.keywords) {
                if (cleanName.contains(kw)) {
                    return age;
                }
            }
        }
        
        // 3단계: 숫자 파싱 시도 (예: "25", "17세", "35살")
        try {
            String numStr = cleanName.replaceAll("[^0-9]", "");
            if (!numStr.isEmpty()) {
                int num = Integer.parseInt(numStr);
                if (num < 6) return INFANT;
                if (num < 8) return CHILD;
                if (num < 14) return ELEMENTARY;
                if (num < 20) return TEENAGER;
                if (num < 30) return TWENTIES;
                if (num < 40) return THIRTIES;
                if (num < 50) return FORTIES;
                if (num < 60) return FIFTIES;
                return SIXTIES_OVER;
            }
        } catch (NumberFormatException e) {
            // 무시
        }
        
        return null;
    }
}
