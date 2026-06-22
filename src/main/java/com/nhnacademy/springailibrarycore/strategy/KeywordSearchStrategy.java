package com.nhnacademy.springailibrarycore.strategy;

import com.nhnacademy.springailibrarycore.domain.SearchType;
import com.nhnacademy.springailibrarycore.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * 제목, 저자, ISBN 등의 문자열 조건을 사용하는 키워드 검색 전략입니다.
 */
@Component
@RequiredArgsConstructor
public class KeywordSearchStrategy implements SearchStrategy {

    private final BookRepository bookRepository;

    @Override
    public SearchType supports() {
        return SearchType.KEYWORD;
    }

    @Override
    public Page<BookSearchResponse> search(
            Pageable pageable,
            BookSearchRequest request
    ) {
        return bookRepository.search(pageable, request);
    }
}
