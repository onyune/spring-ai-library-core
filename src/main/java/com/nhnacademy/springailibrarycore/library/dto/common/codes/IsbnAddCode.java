package com.nhnacademy.springailibrarycore.library.dto.common.codes;

public enum IsbnAddCode {
    GENERAL("0", "교양", new String[]{"교양", "일반"}),
    ACADEMIC("1", "전공", new String[]{"전공", "학술전공"}),
    PRACTICAL("2", "실용", new String[]{"실용", "기술실용"}),
    CHILD("5", "아동", new String[]{"아동", "어린이", "유아"}),
    ELEMENTARY("6", "초등학습", new String[]{"초등", "초등학생", "초등학습"}),
    SECONDARY("7", "중고등학습", new String[]{"중고등", "청소년학습", "수험서"}),
    PROFESSIONAL("8", "전문", new String[]{"전문", "기술전문"});

    private final String code;
    private final String name;
    private final String[] keywords;

    IsbnAddCode(String code, String name, String[] keywords) {
        this.code = code;
        this.name = name;
        this.keywords = keywords;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public static IsbnAddCode fromName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String clean = name.replaceAll("\\s+", "").toLowerCase();
        
        for (IsbnAddCode code : values()) {
            if (clean.equals(code.name) || clean.equals(code.code)) {
                return code;
            }
        }
        
        for (IsbnAddCode code : values()) {
            for (String kw : code.keywords) {
                if (clean.contains(kw)) {
                    return code;
                }
            }
        }
        return null;
    }
}
