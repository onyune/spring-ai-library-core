package com.nhnacademy.springailibrarycore.library.dto.common.codes;

import java.util.Arrays;

public enum GenderCode {
    MALE("0", "남성", new String[]{"남성", "남자", "남", "male"}),
    FEMALE("1", "여성", new String[]{"여성", "여자", "여", "female"});

    private final String code;
    private final String name;
    private final String[] keywords;

    GenderCode(String code, String name, String[] keywords) {
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

    public static GenderCode fromName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String clean = name.replaceAll("\\s+", "").toLowerCase();
        
        // Exact match
        for (GenderCode gender : values()) {
            if (clean.equals(gender.name) || clean.equals(gender.code)) {
                return gender;
            }
        }
        
        // Keyword match
        for (GenderCode gender : values()) {
            for (String kw : gender.keywords) {
                if (clean.contains(kw)) {
                    return gender;
                }
            }
        }
        return null;
    }
}
