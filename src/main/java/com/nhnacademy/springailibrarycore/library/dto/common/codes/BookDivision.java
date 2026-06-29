package com.nhnacademy.springailibrarycore.library.dto.common.codes;

public enum BookDivision {
    BIG("big", "큰글씨도서", new String[]{"큰글", "큰글자", "큰글씨"}),
    OVERSEA("oversea", "국외도서", new String[]{"국외", "해외", "외국", "원서"});

    private final String code;
    private final String name;
    private final String[] keywords;

    BookDivision(String code, String name, String[] keywords) {
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

    public static BookDivision fromName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String clean = name.replaceAll("\\s+", "").toLowerCase();
        
        for (BookDivision div : values()) {
            if (clean.equals(div.name) || clean.equals(div.code)) {
                return div;
            }
        }
        
        for (BookDivision div : values()) {
            for (String kw : div.keywords) {
                if (clean.contains(kw)) {
                    return div;
                }
            }
        }
        return null;
    }
}
