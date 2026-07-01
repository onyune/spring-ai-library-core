package com.nhnacademy.springailibrarycore.book.strategy.impl;

import static com.nhnacademy.springailibrarycore.config.CacheConfig.CACHE_VECTOR_SEARCH;

import com.nhnacademy.springailibrarycore.book.domain.SearchType;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchPageResult;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.repository.BookRepository;
import com.nhnacademy.springailibrarycore.book.service.agent.embedding.EmbeddingSubAgent;
import com.nhnacademy.springailibrarycore.book.service.cache.SemanticCacheService;
import com.nhnacademy.springailibrarycore.book.strategy.SearchStrategy;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
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
    private final SemanticCacheService semanticCacheService;


    @Override
    public SearchType supports() {
        return SearchType.VECTOR;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_VECTOR_SEARCH, key = "#request.keyword() + '_' + #pageable.pageNumber")
    // L1 -> caffeine , L2-> redis , L3 -> sematic
    public BookSearchPageResult search(Pageable pageable, BookSearchRequest request) {
        if (!StringUtils.hasText(request.keyword())) {
            return new BookSearchPageResult(List.of(), 0);
        }

        String keyword = request.keyword().trim();
        float[] queryVector = request.vector() != null
                ? request.vector()
                : embeddingSubAgent.getEmbedding(keyword).getVector();

        /* ============ 시맨틱 캐시 조회 ============*/
        Optional<List<BookSearchResponse>> cachedResult = semanticCacheService.findCachedResult(
                SearchType.VECTOR,
                keyword,
                queryVector
        );
        
        if (cachedResult.isPresent()) {
            List<BookSearchResponse> list = cachedResult.get();
            return BookSearchPageResult.paginate(list, pageable);
        }

        BookSearchRequest dbRequest = new BookSearchRequest(
                keyword,
                request.isbn(),
                SearchType.VECTOR,
                queryVector
        );
        log.info("[VectorSearchStrategy] 벡터 서치 시작 - keyword: {}", keyword);
        BookSearchPageResult result = bookRepository.vectorSearch(pageable, dbRequest);
        
        /* ============ 결과를 시맨틱 캐시에 저장 (첫 페이지 또는 전체 결과를 캐싱할지 결정 필요, 임시로 현재 결과 저장) ============*/
        if (!result.getContent().isEmpty() && pageable.getPageNumber() == 0) {
            semanticCacheService.save(
                    SearchType.VECTOR,
                    keyword,
                    queryVector,
                    result.getContent()
            );
        }
        
        return result;
    }
}
