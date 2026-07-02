package com.nhnacademy.springailibrarycore.book.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 도서 검색 타입 Enum
 */
@Getter
@RequiredArgsConstructor
public enum SearchType {
    KEYWORD("keyword"),
    VECTOR("vector"),
    HYBRID("hybrid"),
    RAG("rag"),
    AUTO("auto");

    private final String value;

    @JsonCreator
    public static SearchType from(String value) {
        return Arrays.stream(SearchType.values())
                .filter(type -> type.value.equalsIgnoreCase(value) || type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElse(KEYWORD);
    }
}
