package com.nhnacademy.springailibrarycore.repository.impl;

import com.nhnacademy.library.core.book.dto.BookSearchRequest;
import com.nhnacademy.library.core.book.dto.BookSearchResponse;
import com.nhnacademy.library.core.book.repository.BookRepositoryCustom;
import com.nhnacademy.library.core.book.repository.search.KeywordBookSearchRepository;
import com.nhnacademy.library.core.book.repository.search.VectorBookSearchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BookRepositoryImpl implements BookRepositoryCustom {

    private final KeywordBookSearchRepository keywordSearchRepository;
    private final VectorBookSearchRepository vectorSearchRepository;

    @Override
    public Page<BookSearchResponse> search(
            Pageable pageable,
            BookSearchRequest request
    ) {
        return keywordSearchRepository.search(pageable, request);
    }

    @Override
    public Page<BookSearchResponse> vectorSearch(
            Pageable pageable,
            BookSearchRequest request
    ) {
        return vectorSearchRepository.search(pageable, request.vector());
    }
}
