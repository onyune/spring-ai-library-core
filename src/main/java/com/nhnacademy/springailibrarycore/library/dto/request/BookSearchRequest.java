package com.nhnacademy.springailibrarycore.library.dto.request;

import lombok.Builder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 도서 검색 API (srchBooks) 호출을 위한 요청 DTO 클래스
 * 도서명, 저자명, 출판사, ISBN, 키워드 등의 검색 파라미터를 AND 결합하여 도서관 소장 데이터를 검색
 *
 * @param title      검색어 - 도서명
 * @param author     검색어 - 저자명
 * @param isbn13     검색어 - 13자리 ISBN
 * @param keyword    검색어 - 도서에 설정된 핵심 키워드. 세미콜론(;)을 구분자로 여러 키워드를 조합하여 검색
 * @param publisher  검색어 - 출판사명
 * @param sort       결과 목록의 정렬 기준 필드입니다.
 * @param order      정렬 방향 (asc: 오름차순, desc: 내림차순)
 * @param exactMatch 단어 완전 일치 검색 여부 (true: 완전 일치, false: 부분 일치)
 * @param pageNo     조회할 결과의 페이지 번호 (기본값: 1)
 * @param pageSize   한 페이지에 노출될 결과 도서 수 (기본값: 10)
 */
@Builder
public record BookSearchRequest(
        String title,
        String author,
        String isbn13,
        String keyword,
        String publisher,
        String sort,        // title, author, pub, pubYear, isbn, loan
        String order,       // asc, desc
        Boolean exactMatch, // true, false
        Integer pageNo,
        Integer pageSize
) {
    public BookSearchRequest {
        if (pageNo == null) pageNo = 1;
        if (pageSize == null) pageSize = 10;
    }

    public MultiValueMap<String, String> toQueryParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        if (title != null && !title.isBlank()) params.add("title", title);
        if (author != null && !author.isBlank()) params.add("author", author);
        if (isbn13 != null && !isbn13.isBlank()) params.add("isbn13", isbn13);
        if (keyword != null && !keyword.isBlank()) params.add("keyword", keyword);
        if (publisher != null && !publisher.isBlank()) params.add("publisher", publisher);
        if (sort != null && !sort.isBlank()) params.add("sort", sort);
        if (order != null && !order.isBlank()) params.add("order", order);
        if (exactMatch != null) params.add("exactMatch", String.valueOf(exactMatch));
        if (pageNo != null) params.add("pageNo", String.valueOf(pageNo));
        if (pageSize != null) params.add("pageSize", String.valueOf(pageSize));

        return params;
    }
}
