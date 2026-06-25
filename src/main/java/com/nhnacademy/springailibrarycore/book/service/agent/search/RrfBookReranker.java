package com.nhnacademy.springailibrarycore.book.service.agent.search;

import com.nhnacademy.springailibrarycore.book.dto.BookSearchPageResult;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.strategy.impl.HybridSearchStrategy;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * HYBRID + RRF SCORE 점수 결과를 바탕으로 5개 도서 추출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RrfBookReranker {
    private static final int RETRIEVAL_K = 100;
    private static final int RERANK_K = 5;
    private static final double RRF_SCORE_THRESHOLD = 0.02;

    private final HybridSearchStrategy hybridSearchStrategy;

    public List<BookSearchResponse> reranker(BookSearchRequest request){
        // ------- HYBRID 전략 으로 도서 리스트 검색 -------
        BookSearchPageResult retrievalResult =
                hybridSearchStrategy.search(
                        PageRequest.of(0, RETRIEVAL_K),
                        request
                );
        // -------- rrf score가 높은 순으로 내림차순 정렬 ----------
        List<BookSearchResponse> rankedBooks = retrievalResult.content()
                .stream()
                .filter(book -> book.getRrfScore() != null)
                .sorted(Comparator.comparing(
                        BookSearchResponse::getRrfScore
                ).reversed())
                .toList();

        // ----------- rrf 점수 하한선 이상으로 5개만 추출 -----------
        List<BookSearchResponse> topBooks = rankedBooks.stream()
                .filter(book ->
                        book.getRrfScore() >= RRF_SCORE_THRESHOLD
                )
                .limit(RERANK_K)
                .toList();

        // --------- 하하선 통과 결과가 없을 시 rrfScore가 높은 순서대로 5개만 추출하는 fallback ---------
        if (topBooks.isEmpty() && !rankedBooks.isEmpty()) {
            topBooks = rankedBooks.stream()
                    .limit(RERANK_K)
                    .toList();
            log.info("[RAG Top-K] 임계값 통과 결과가 없어 상위 {}권을 fallback으로 사용", topBooks.size());
        }

        log.info("[RAG Top-K] Retrieval {}권 → Rerank {}권", retrievalResult.content().size(), topBooks.size());
        return topBooks;
    }
}
