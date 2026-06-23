package com.nhnacademy.springailibrarycore.book.strategy.impl;

import com.nhnacademy.springailibrarycore.book.domain.SearchType;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.strategy.SearchStrategy;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * RAG 프롬프트에 전달할 도서 후보를 선별하는 검색 전략입니다.
 *
 * 하이브리드 검색으로 Retrieval 후보를 확보하고, RRF 임계값 필터링과 정렬을 거쳐
 * 상위 Rerank K개의 도서만 반환합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RagSearchStrategy implements SearchStrategy {

    private static final int RETRIEVAL_K = 100;
    private static final int RERANK_K = 5;
    private static final double RRF_SCORE_THRESHOLD = 0.02;

    private final HybridSearchStrategy hybridSearchStrategy;

    @Override
    public SearchType supports() {
        return SearchType.RAG;
    }

    @Override
    public Page<BookSearchResponse> search(
            Pageable pageable,
            BookSearchRequest request
    ) {
        Page<BookSearchResponse> retrievalResult =
                hybridSearchStrategy.search(
                        PageRequest.of(0, RETRIEVAL_K),
                        request
                );

        List<BookSearchResponse> rankedBooks = retrievalResult.getContent()
                .stream()
                .filter(book -> book.rrfScore() != null)
                .sorted(Comparator.comparing(
                        BookSearchResponse::rrfScore
                ).reversed())
                .toList();

        List<BookSearchResponse> topBooks = rankedBooks.stream()
                .filter(book ->
                        book.rrfScore() >= RRF_SCORE_THRESHOLD
                )
                .limit(RERANK_K)
                .toList();

        if (topBooks.isEmpty() && !rankedBooks.isEmpty()) {
            topBooks = rankedBooks.stream()
                    .limit(RERANK_K)
                    .toList();
            log.info(
                    "[RAG Top-K] 임계값 통과 결과가 없어 상위 {}권을 fallback으로 사용",
                    topBooks.size()
            );
        }

        log.info(
                "[RAG Top-K] Retrieval {}권 → Rerank {}권",
                retrievalResult.getNumberOfElements(),
                topBooks.size()
        );

        return new PageImpl<>(topBooks);
    }
}
