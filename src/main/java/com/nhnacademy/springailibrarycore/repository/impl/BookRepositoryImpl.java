package com.nhnacademy.springailibrarycore.repository.impl;


import com.nhnacademy.springailibrarycore.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.repository.BookRepositoryCustom;
import com.nhnacademy.springailibrarycore.repository.impl.search.KeywordBookSearchRepository;
import com.nhnacademy.springailibrarycore.repository.impl.search.VectorBookSearchRepository;
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
