package com.nhnacademy.springailibrarycore.library.dto.common.codes;

public enum KdcCode {
    GENERAL("0", "총류", new String[]{"총류", "백과사전"}),
    PHILOSOPHY("1", "철학", new String[]{"철학", "심리", "윤리"}),
    RELIGION("2", "종교", new String[]{"종교", "기독교", "불교", "천주교", "성경"}),
    SOCIAL_SCIENCE("3", "사회과학", new String[]{"사회과학", "정치", "경제", "사회", "법률", "교육"}),
    NATURAL_SCIENCE("4", "자연과학", new String[]{"자연과학", "과학", "수학", "물리", "생물", "화학"}),
    TECHNOLOGY("5", "기술과학", new String[]{"기술과학", "의학", "공학", "요리", "생활과학", "의류"}),
    ARTS("6", "예술", new String[]{"예술", "미술", "음악", "체육", "영화", "스포츠"}),
    LANGUAGE("7", "언어", new String[]{"언어", "어학", "영어", "국어", "외국어"}),
    LITERATURE("8", "문학", new String[]{"문학", "소설", "시", "수필", "에세이", "희곡"}),
    HISTORY("9", "역사", new String[]{"역사", "지리", "세계사", "한국사", "여행"});

    private final String code;
    private final String name;
    private final String[] keywords;

    KdcCode(String code, String name, String[] keywords) {
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

    public static KdcCode fromName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String clean = name.replaceAll("\\s+", "").toLowerCase();
        
        for (KdcCode code : values()) {
            if (clean.equals(code.name) || clean.equals(code.code)) {
                return code;
            }
        }
        
        for (KdcCode code : values()) {
            for (String kw : code.keywords) {
                if (clean.contains(kw)) {
                    return code;
                }
            }
        }
        return null;
    }
}
