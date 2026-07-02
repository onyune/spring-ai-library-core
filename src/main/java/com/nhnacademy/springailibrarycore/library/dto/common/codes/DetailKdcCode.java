package com.nhnacademy.springailibrarycore.library.dto.common.codes;

public enum DetailKdcCode {
    COMPUTERS("00", "컴퓨터", new String[]{"컴퓨터", "it"}),
    LIBRARY_SCIENCE("02", "도서관학", new String[]{"도서관학", "문헌정보"}),
    METAPHYSICS("11", "형이상학", new String[]{"형이상학"}),
    EPISTEMOLOGY("12", "인식론", new String[]{"인식론"}),
    PSYCHOLOGY("18", "심리학", new String[]{"심리"}),
    ETHICS("19", "윤리학", new String[]{"윤리", "도덕"}),
    BUDDHISM("22", "불교", new String[]{"불교"}),
    CHRISTIANITY("23", "기독교", new String[]{"기독교", "개신교"}),
    ECONOMICS("32", "경제학", new String[]{"경제", "경영"}),
    POLITICS("34", "정치학", new String[]{"정치"}),
    LAW("36", "법학", new String[]{"법률", "법학"}),
    EDUCATION("37", "교육학", new String[]{"교육"}),
    MATHEMATICS("41", "수학", new String[]{"수학"}),
    PHYSICS("42", "물리학", new String[]{"물리"}),
    CHEMISTRY("43", "화학", new String[]{"화학"}),
    BIOLOGY("47", "생물학", new String[]{"생물", "생명과학"}),
    ENGINEERING("50", "공학", new String[]{"공학"}),
    MEDICINE("51", "의학", new String[]{"의학", "약학"}),
    AGRICULTURE("52", "농업", new String[]{"농업"}),
    COOKING("59", "요리학", new String[]{"요리", "조리"}),
    ARCHITECTURE("61", "건축학", new String[]{"건축"}),
    FINE_ARTS("65", "미술", new String[]{"조각", "미술"}),
    MUSIC("67", "음악", new String[]{"음악"}),
    SPORTS("69", "체육", new String[]{"체육", "스포츠"}),
    KOREAN("71", "국어", new String[]{"국어", "한국어"}),
    ENGLISH("74", "영어", new String[]{"영어", "영문"}),
    KOREAN_LITERATURE("81", "한국문학", new String[]{"한국문학", "소설", "수필"}),
    ENGLISH_LITERATURE("84", "영미문학", new String[]{"영미문학"}),
    KOREAN_HISTORY("91", "한국사", new String[]{"아시아사", "한국사", "국사"}),
    WORLD_HISTORY("92", "세계사", new String[]{"유럽사", "세계사"}),
    GEOGRAPHY("98", "지리학", new String[]{"지리", "여행"});

    private final String code;
    private final String name;
    private final String[] keywords;

    DetailKdcCode(String code, String name, String[] keywords) {
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

    public static DetailKdcCode fromName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String clean = name.replaceAll("\\s+", "").toLowerCase();
        
        for (DetailKdcCode code : values()) {
            if (clean.equals(code.name) || clean.equals(code.code)) {
                return code;
            }
        }
        
        for (DetailKdcCode code : values()) {
            for (String kw : code.keywords) {
                if (clean.contains(kw)) {
                    return code;
                }
            }
        }
        return null;
    }
}
