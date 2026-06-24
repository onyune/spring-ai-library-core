package com.nhnacademy.springailibrarycore.book.strategy.impl;

import com.nhnacademy.springailibrarycore.book.domain.SearchType;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.repository.BookRepository;
import com.nhnacademy.springailibrarycore.book.service.agent.embedding.EmbeddingSubAgent;
import com.nhnacademy.springailibrarycore.book.service.agent.search.RrfFusionSubAgent;
import com.nhnacademy.springailibrarycore.book.strategy.SearchStrategy;
import com.nhnacademy.springailibrarycore.util.PageableUtils;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 키워드 검색과 벡터 검색 결과를 RRF 점수로 결합하는 하이브리드 검색 전략입니다.
 * 각 검색 결과의 순위를 점수로 환산해 합산하고, 종합 점수가 높은 도서부터
 * 반환합니다.
 */
@Component
@RequiredArgsConstructor
public class HybridSearchStrategy implements SearchStrategy {

    /**
     * CANDIDATE_SIZE = 후보 도서 수
     */
    private static final int CANDIDATE_SIZE = 100;

    private final BookRepository bookRepository;
    private final EmbeddingSubAgent embeddingSubAgent;
    private final RrfFusionSubAgent rrfFusionSubAgent;

    @Override
    public SearchType supports() {
        return SearchType.HYBRID;
    }

    @Override
    @Cacheable(value = "hybridSearchCache", key = "#request.searchType().name() + '_' +#request.keyword() + '_' + #pageable.pageNumber")
    public Page<BookSearchResponse> search(
            Pageable pageable,
            BookSearchRequest request
    ) {
        if (!StringUtils.hasText(request.keyword())) {
            return Page.empty(pageable);
        }

        String keyword = request.keyword().trim();
        Pageable candidatePage = PageRequest.of(0, CANDIDATE_SIZE);

        // ---------- KEYWORD 검색 -------------
        BookSearchRequest keywordRequest = new BookSearchRequest(
                keyword,
                request.isbn(),
                SearchType.KEYWORD,
                null,
                request.warmUp()
        );

        List<BookSearchResponse> keywordResults =
                bookRepository.search(candidatePage, keywordRequest).getContent();

        // ---------------- VECTOR 검색 --------------
        float[] queryVector = request.vector() != null
                ? request.vector()
                : embeddingSubAgent.getEmbedding(keyword);
        BookSearchRequest vectorRequest = new BookSearchRequest(
                keyword,
                request.isbn(),
                SearchType.VECTOR,
                queryVector,
                request.warmUp()
        );
        List<BookSearchResponse> vectorResults =
                bookRepository.vectorSearch(candidatePage, vectorRequest).getContent();

        // ---------------- (KEYWORD + VECTOR) -> rrf 점수를 반영한 도서 리스트 반환 ---------------
        List<BookSearchResponse> rankedResults =
                rrfFusionSubAgent.fuse(keywordResults, vectorResults);

        return PageableUtils.toPage(rankedResults, pageable);
    }
}
