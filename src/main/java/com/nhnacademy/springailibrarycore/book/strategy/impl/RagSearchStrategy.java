package com.nhnacademy.springailibrarycore.book.strategy.impl;

import com.nhnacademy.springailibrarycore.book.domain.SearchType;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.service.agent.embedding.EmbeddingSubAgent;
import com.nhnacademy.springailibrarycore.book.service.agent.recommendation.BookRecommendationAgent;
import com.nhnacademy.springailibrarycore.book.service.cache.SemanticCacheService;
import com.nhnacademy.springailibrarycore.book.strategy.SearchStrategy;
import com.nhnacademy.springailibrarycore.book.service.agent.search.RrfBookReranker;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * RAG 프롬프트에 전달할 도서 후보를 선별하는 검색 전략입니다.
 *
 * 하이브리드 검색으로 Retrieval 후보를 확보하고, RRF 임계값 필터링과 정렬을 거쳐
 * 상위 Rerank K개의 도서만 반환합니다.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RagSearchStrategy implements SearchStrategy {
    private final RrfBookReranker rrfBookReranker;
    private final EmbeddingSubAgent embeddingSubAgent;
    private final BookRecommendationAgent bookRecommendationAgent;
    private final SemanticCacheService semanticCacheService;


    @Override
    public SearchType supports() {
        return SearchType.RAG;
    }

    @Override
    public Page<BookSearchResponse> search(
            Pageable pageable,
            BookSearchRequest request
    ) {
        String normalizedQuestion = request.keyword().trim();
        float[] questionVector = request.vector() != null
                ? request.vector()
                : embeddingSubAgent.getEmbedding(normalizedQuestion).vector();

        /* ============ 캐시 조회 ============*/

        Optional<List<BookSearchResponse>> cachedResult = semanticCacheService.findCachedResult(
                normalizedQuestion,
                questionVector
        );
        // 캐시 존재하면 반환
        if(cachedResult.isPresent()){
            return new PageImpl<>(cachedResult.get(), pageable, cachedResult.get().size());
        }

        /* ============ 캐시 MISS → Hybrid Retrieval + RRF Rerank ============*/
        log.info("[VectorCache] Cache MISS - Hybrid 검색 후 캐시 저장");
        BookSearchRequest vectorizedRequest = new BookSearchRequest(
                normalizedQuestion,
                request.isbn(),
                request.searchType(),
                questionVector,
                request.warmUp()
        );
        // --------------- RRF Rerank: 상위 5권 추출 ---------------
        List<BookSearchResponse> topBooks = rrfBookReranker.reranker(vectorizedRequest);

        if (topBooks.isEmpty()) {
            log.info("[RAG] 검색 결과 없음 - 빈 페이지 반환");
            return Page.empty(pageable);
        }

        // ---------- AI 추천 사유 부여 ----------
        List<BookSearchResponse> enrichedBooks = bookRecommendationAgent.enrich(normalizedQuestion, topBooks);

        /* ============ 결과를 캐시에 저장 ============*/
        if(!enrichedBooks.isEmpty()){
            semanticCacheService.save(
                    normalizedQuestion,
                    questionVector,
                    enrichedBooks
            );
        }

        return new PageImpl<>(enrichedBooks, pageable, enrichedBooks.size());
    }
}
