package com.nhnacademy.springailibrarycore.book.strategy.impl;

import static com.nhnacademy.springailibrarycore.config.CacheConfig.CACHE_HYBRID_SEARCH;

import com.nhnacademy.springailibrarycore.book.domain.SearchType;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchPageResult;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.service.agent.embedding.EmbeddingSubAgent;
import com.nhnacademy.springailibrarycore.book.service.agent.search.RrfFusionSubAgent;
import com.nhnacademy.springailibrarycore.book.service.cache.SemanticCacheService;
import com.nhnacademy.springailibrarycore.book.strategy.SearchStrategy;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 키워드 검색과 벡터 검색 결과를 RRF 점수로 결합하는 하이브리드 검색 전략입니다.
 * 각 검색 결과의 순위를 점수로 환산해 합산하고, 종합 점수가 높은 도서부터
 * 반환합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HybridSearchStrategy implements SearchStrategy {

    /**
     * candidateSize = 후보 도서 수
     */
    @Value("${hybrid.search.candidate}")
    private int candidateSize;

    private final EmbeddingSubAgent embeddingSubAgent;
    private final RrfFusionSubAgent rrfFusionSubAgent;
    private final SemanticCacheService semanticCacheService;
    private final KeywordSearchStrategy keywordSearchStrategy;
    private final VectorSearchStrategy vectorSearchStrategy;

    private final Executor virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public SearchType supports() {
        return SearchType.HYBRID;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_HYBRID_SEARCH, key = "#request.keyword() + '_' + #pageable.pageNumber")
    public BookSearchPageResult search(
            Pageable pageable,
            BookSearchRequest request
    ) {
        if (!StringUtils.hasText(request.keyword())) {
            return new BookSearchPageResult(List.of(), 0);
        }

        String keyword = request.keyword().trim();
        Pageable candidatePage = PageRequest.of(0, candidateSize);
        float[] queryVector = request.vector() != null
                ? request.vector()
                : embeddingSubAgent.getEmbedding(keyword).getVector();
                
        /* ============ 시맨틱 캐시 조회 (초기 단계) ============*/
        Optional<List<BookSearchResponse>> cachedResult = semanticCacheService.findCachedResult(
                SearchType.HYBRID,
                keyword,
                queryVector
        );
        
        List<BookSearchResponse> fusedList;
        
        if (cachedResult.isPresent()) {
            fusedList = cachedResult.get();
        } else { // 캐싱된 결과가 없을 때 키워드 + 벡터 병렬 서치 후 rrf 검사
            fusedList = executeParallelSearchAndFuse(
                    keyword,
                    request.isbn(),
                    queryVector,
                    candidatePage,
                    request.chatId()
            );
            
            /* ============ 결과를 시맨틱 캐시에 저장 ============*/
            if (!fusedList.isEmpty()) {
                semanticCacheService.save(
                        SearchType.HYBRID,
                        keyword,
                        queryVector,
                        fusedList
                );
            }
        }

        // ---------------- 페이징 처리 ----------------
        return BookSearchPageResult.paginate(fusedList, pageable);
    }

    /**
     * 키워드 전략 + 벡터 전략 병렬 실행 및 rrf 점수 계산까지
     * @param keyword 사용자 쿼리
     * @param isbn 들어오는 isbn
     * @param queryVector 사용자 쿼리에대한 임베딩 값
     * @param candidatePage 페이징
     * @return rrf 점수 계산 한 도서 리스트
     */

    private List<BookSearchResponse> executeParallelSearchAndFuse(
            String keyword,
            String isbn,
            float[] queryVector,
            Pageable candidatePage,
            Long chatId
    ) {
        // ---------- KEYWORD 전략 (가상 스레드 기반 병렬 비동기 실행) -------------
        BookSearchRequest keywordRequest = new BookSearchRequest(
                keyword, isbn, SearchType.KEYWORD, null,null
        );
        CompletableFuture<List<BookSearchResponse>> keywordFuture = CompletableFuture.supplyAsync(
                () -> keywordSearchStrategy.search(candidatePage, keywordRequest).getContent(),
                virtualThreadExecutor
        );

        // ---------------- VECTOR 전략 (가상 스레드 기반 병렬 비동기 실행 + 4초 타임아웃/폴백) --------------
        BookSearchRequest vectorRequest = new BookSearchRequest(
                keyword, isbn, SearchType.VECTOR, queryVector, chatId
        );
        CompletableFuture<List<BookSearchResponse>> vectorFuture = CompletableFuture.supplyAsync(
                () -> vectorSearchStrategy.search(candidatePage, vectorRequest).getContent(),
                virtualThreadExecutor
        ).orTimeout(4, TimeUnit.SECONDS)
         .exceptionally(ex -> {
             log.warn("[Hybrid Search] 벡터 검색 4초 타임아웃 또는 예외 발생. 키워드 검색 결과만으로 Fallback 합니다.", ex);
             return Collections.emptyList();
         });

        // 두 작업이 모두 끝날 때까지 대기 (가장 느린 쿼리 시간, 최대 4초에 수렴)
        CompletableFuture.allOf(keywordFuture, vectorFuture).join();
        
        return rrfFusionSubAgent.fuse(keywordFuture.join(), vectorFuture.join());
    }
}
