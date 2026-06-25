package com.nhnacademy.springailibrarycore.book.dto;

import java.util.List;

/**
 * PageImpl 역직렬화 이슈를 방지하기 위해 검색 결과를 캐싱하고 전달할 때 사용하는 DTO 레코드입니다.
 */
public record BookSearchPageResult(
        List<BookSearchResponse> content,
        long totalElements
) {
}
