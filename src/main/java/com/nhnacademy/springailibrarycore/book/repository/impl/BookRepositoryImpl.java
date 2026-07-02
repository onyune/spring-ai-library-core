package com.nhnacademy.springailibrarycore.book.repository.impl;


import com.nhnacademy.springailibrarycore.book.dto.BookSearchPageResult;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.book.repository.BookRepositoryCustom;
import com.nhnacademy.springailibrarycore.book.repository.impl.search.KeywordBookSearchRepository;
import com.nhnacademy.springailibrarycore.book.repository.impl.search.VectorBookSearchRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BookRepositoryImpl implements BookRepositoryCustom {

    private final KeywordBookSearchRepository keywordSearchRepository;
    private final VectorBookSearchRepository vectorSearchRepository;

    @Override
    public BookSearchPageResult search(
            Pageable pageable,
            BookSearchRequest request
    ) {
        return keywordSearchRepository.search(pageable, request);
    }

    @Override
    public BookSearchPageResult vectorSearch(
            Pageable pageable,
            BookSearchRequest request
    ) {
        return vectorSearchRepository.search(pageable, request.vector());
    }

    @Override
    public List<com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse> findPersonalizedBooks(float[] vector, int limit) {
        return vectorSearchRepository.findPersonalizedBooks(vector, limit);
    }

    @Override
    public List<float[]> findEmbeddingByBookIds(List<Long> bookIds) {
        return vectorSearchRepository.findEmbeddingsByBookIds(bookIds);
    }

    @Override
    public java.util.Map<Long, float[]> findEmbeddingMapByBookIds(List<Long> bookIds) {
        return vectorSearchRepository.findEmbeddingMapByBookIds(bookIds);
    }
}
