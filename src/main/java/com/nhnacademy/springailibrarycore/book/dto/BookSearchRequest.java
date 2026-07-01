package com.nhnacademy.springailibrarycore.book.dto;

import com.nhnacademy.springailibrarycore.book.domain.SearchType;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.Objects;

/**
 * 전체 검색 파이프라인에서 사용하는 요청 DTO.
 */
public record BookSearchRequest(
        @Size(max = 100)
        String keyword,

        @Size(max = 20)
        String isbn,

        SearchType searchType,
        float[] vector,
        Long chatId
) {
    public BookSearchRequest {
        searchType = searchType == null ? SearchType.KEYWORD : searchType;
    }

    public BookSearchRequest(String keyword, String isbn) {
        this(keyword, isbn, SearchType.KEYWORD, null, null);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BookSearchRequest that)) {
            return false;
        }
        return Objects.equals(keyword, that.keyword)
                && Objects.equals(isbn, that.isbn)
                && searchType == that.searchType
                && Arrays.equals(vector, that.vector)
                && Objects.equals(chatId, that.chatId);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(keyword, isbn, searchType, chatId);
        return 31 * result + Arrays.hashCode(vector);
    }
}
