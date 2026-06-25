package com.nhnacademy.springailibrarycore.book.strategy.impl;

import static com.nhnacademy.springailibrarycore.config.RedisCacheConfig.CACHE_BOOK_SEARCH;

import com.nhnacademy.springailibrarycore.book.domain.SearchType;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.repository.BookRepository;
import com.nhnacademy.springailibrarycore.book.service.agent.embedding.EmbeddingSubAgent;
import com.nhnacademy.springailibrarycore.book.strategy.SearchStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 벡터 전략 검색
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VectorSearchStrategy implements SearchStrategy {

    private final BookRepository bookRepository;
    private final EmbeddingSubAgent embeddingSubAgent;


    @Override
    public SearchType supports() {
        return SearchType.VECTOR;
    }

    @Override
    @Cacheable(value = CACHE_BOOK_SEARCH, key = "#request.searchType().name() + '_' +#request.keyword() + '_' + #pageable.pageNumber")
    // 검색어와 페이지 번호가 완전히 똑같을 때만 캐시를 탐
    public Page<BookSearchResponse> search(Pageable pageable, BookSearchRequest request) {
        if (!StringUtils.hasText(request.keyword())) {
            return Page.empty(pageable);
        }

        String keyword = request.keyword().trim();
        float[] queryVector = request.vector() != null
                ? request.vector()
                : embeddingSubAgent.getEmbedding(keyword).vector();


        BookSearchRequest dbRequest = new BookSearchRequest(
                keyword,
                request.isbn(),
                SearchType.VECTOR,
                queryVector,
                request.warmUp()
        );

        return bookRepository.vectorSearch(pageable, dbRequest);
    }
}
