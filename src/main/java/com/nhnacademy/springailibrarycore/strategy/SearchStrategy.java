package com.nhnacademy.springailibrarycore.strategy;

import com.nhnacademy.springailibrarycore.domain.SearchType;
import com.nhnacademy.springailibrarycore.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.dto.BookSearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 검색 유형별 알고리즘을 동일한 방식으로 실행하기 위한 Strategy 인터페이스입니다.
 *
 * 각 구현체는 자신이 지원하는 {@link SearchType}과 해당 검색 로직을 제공합니다.
 */
public interface SearchStrategy {

    SearchType supports();

    Page<BookSearchResponse> search(
            Pageable pageable,
            BookSearchRequest request
    );
}
