package com.nhnacademy.springailibrarycore.book.strategy.impl;

import static com.nhnacademy.springailibrarycore.config.CacheConfig.CACHE_KEYWORD_SEARCH;

import com.nhnacademy.springailibrarycore.book.domain.SearchType;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchPageResult;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.book.repository.BookRepository;
import com.nhnacademy.springailibrarycore.book.service.agent.search.QueryAnalyzerSubAgent;
import com.nhnacademy.springailibrarycore.book.strategy.SearchStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 제목, 저자, ISBN 등의 문자열 조건을 사용하는 키워드 검색 전략입니다.
 */
@Component
@RequiredArgsConstructor
public class KeywordSearchStrategy implements SearchStrategy {

    private final BookRepository bookRepository;
    private final QueryAnalyzerSubAgent queryAnalyzerSubAgent;

    @Override
    public SearchType supports() {
        return SearchType.KEYWORD;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_KEYWORD_SEARCH, key = "#request.keyword() + '_' + #pageable.pageNumber")
    public BookSearchPageResult search(
            Pageable pageable,
            BookSearchRequest request
    ) {
        String rawKeyword = request.keyword();
        
        // 불용어 제거 및 핵심 명사 추출
        String extractedKeyword = queryAnalyzerSubAgent.extractKeywords(rawKeyword);
        
        // 정제된 키워드로 새로운 DTO 생성
        BookSearchRequest refinedRequest = new BookSearchRequest(
                extractedKeyword,
                request.isbn(),
                request.searchType(),
                request.vector()
        );
        
        return bookRepository.search(pageable, refinedRequest);
    }
}
