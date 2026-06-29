package com.nhnacademy.springailibrarycore.book.service.agent.search;

import com.nhnacademy.springailibrarycore.book.dto.BookFeedbackStatistics;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchPageResult;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.service.FeedbackInternalService;
import com.nhnacademy.springailibrarycore.book.strategy.impl.HybridSearchStrategy;
import com.nhnacademy.springailibrarycore.book.service.preference.UserPreferenceService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * HYBRID + RRF SCORE 점수 결과를 바탕으로 5개 도서 추출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RrfBookReranker {

    @Value("${rrf.retrieval.k}")
    private int retrieval_k;
    @Value("${rrf.rerank.k}")
    private int rerank_k = 5;
    @Value("${rrf.score.threshold}")
    private double rrfScoreThreshold = 0.013;

    private final HybridSearchStrategy hybridSearchStrategy;
    private final UserPreferenceService userPreferenceService;
    private final FeedbackInternalService feedbackInternalService;

    public List<BookSearchResponse> reranker(BookSearchRequest request){
        // ------- HYBRID 전략 으로 도서 리스트 검색 -------
        BookSearchPageResult retrievalResult =
                hybridSearchStrategy.search(
                        PageRequest.of(0, retrieval_k),
                        request
                );

        List<BookSearchResponse> candidates = retrievalResult.getContent();
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        // ------- 선호도 벡터 기반 점수 추가 (가중치 0.3) -------
        if (request.chatId() != null) {
            List<Long> candidateIds = candidates.stream().map(BookSearchResponse::getId).toList();
            Map<Long, Double> personalizationScores = userPreferenceService.getPersonalizationScores(candidateIds, request.chatId());

            candidates = candidates.stream().map(book -> {
                Double pScore = personalizationScores.getOrDefault(book.getId(), 0.0);
                if (pScore > 0.0) {
                    double finalScore = (book.getRrfScore() != null ? book.getRrfScore() : 0.0) + (pScore * 0.3);
                    return book.withRrfScore(finalScore).withPersonalizationScore(pScore);
                }
                return book;
            }).toList();
        } else {
            // ------- 글로벌 피드백 통계 기반 점수 가산 (가중치 0.5) -------
            List<Long> bookIds = candidates.stream().map(BookSearchResponse::getId).toList();
            Map<Long, BookFeedbackStatistics> statsMap = feedbackInternalService.getBooksFeedbackStats(bookIds);

            candidates = candidates.stream().map(book -> {
                BookFeedbackStatistics stats = statsMap.get(book.getId());

                if (stats != null && stats.totalCount() >= 5) {
                    double feedbackBonus = stats.score() * 0.5;
                    double finalScore = (book.getRrfScore() != null ? book.getRrfScore() : 0.0) + feedbackBonus;
                    return book.withRrfScore(finalScore);
                }
                return book;
            }).toList();
        }


        // -------- rrf score가 높은 순으로 내림차순 정렬 ----------
        List<BookSearchResponse> rankedBooks = candidates.stream()
                .filter(book -> book.getRrfScore() != null)
                .sorted(Comparator.comparing(
                        BookSearchResponse::getRrfScore
                ).reversed())
                .toList();

        // ----------- rrf 점수 하한선 이상으로 5개만 추출 -----------
        List<BookSearchResponse> topBooks = rankedBooks.stream()
                .filter(book ->
                        book.getRrfScore() >= rrfScoreThreshold
                )
                .limit(rerank_k)
                .toList();

        // --------- 하한선 통과 결과가 없을 시 rrfScore가 높은 순서대로 5개만 추출하는 fallback ---------
        if (topBooks.isEmpty() && !rankedBooks.isEmpty()) {
            topBooks = rankedBooks.stream()
                    .limit(rerank_k)
                    .toList();
            log.info("[RAG Top-K] 임계값 통과 결과가 없어 상위 {}권을 fallback으로 사용", topBooks.size());
        }else{
            log.info("[RAG Top-K] 임계값 통과:{}", rankedBooks.size());
        }

        log.info("[RAG Top-K] Retrieval {}권 → Rerank {}권", retrievalResult.getContent().size(), topBooks.size());
        return topBooks;
    }
}
