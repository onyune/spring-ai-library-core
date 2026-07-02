package com.nhnacademy.springailibrarycore.book.service.agent.search;

import com.nhnacademy.springailibrarycore.book.dto.FeedbackStats;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchPageResult;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.service.preference.UserPreferenceService;
import com.nhnacademy.springailibrarycore.book.strategy.impl.HybridSearchStrategy;
import com.nhnacademy.springailibrarycore.book.client.CohereRerankClient;
import com.nhnacademy.springailibrarycore.telegram.client.TelegramFeedbackClient;
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

    @Value("${hybrid.search.candidate}")
    private int retrieval_k;
    @Value("${rrf.business.filter.k}")
    private int businessFilterK;
    @Value("${rrf.score.threshold}")
    private double rrfScoreThreshold;

    private final HybridSearchStrategy hybridSearchStrategy;
    private final UserPreferenceService userPreferenceService;
    private final TelegramFeedbackClient telegramFeedbackClient;
    private final CohereRerankClient cohereRerankClient;

    public List<BookSearchResponse> reranker(BookSearchRequest request) {
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
        // -------- 2차망: 가중치 부여 global or personalization (Business Reranking) ------------
        candidates = applyAdditionalScores(candidates, request.chatId());

        // 비즈니스 점수로 30권(rerank_k) 압축
        List<BookSearchResponse> businessFilteredBooks = filterAndLimitCandidates(candidates);
        
        if (businessFilteredBooks.isEmpty()) {
            return List.of();
        }

        // -------- 3차망: Cross-Encoder (Cohere API) 정밀 리랭킹 ------------
        // 책 제목과 소개글을 묶어서 Cohere에 전송
        List<String> documentTexts = businessFilteredBooks.stream()
                .map(book -> book.getTitle() + " " + (book.getBookContent() != null ? book.getBookContent() : ""))
                .toList();

        // Cohere가 가장 관련성 높다고 판단한 상위 20개의 원본 인덱스 반환
        List<Integer> bestIndexes = cohereRerankClient.rerank(request.keyword(), documentTexts);

        // 인덱스를 기반으로 최종 20권 필터링
        List<BookSearchResponse> topBooks = bestIndexes.stream()
                .map(businessFilteredBooks::get)
                .toList();

        log.info("[RAG Top-K] Retrieval {}권 → Business Rerank {}권 → CrossEncoder {}권", 
                 retrievalResult.getContent().size(), businessFilteredBooks.size(), topBooks.size());
                 
        return topBooks;
    }

    /**
     * chatId가 존재하면 선호도 기반, 없으면 글로벌 피드백 기반
     *
     * @param candidates 하이브리드 된 검색 결과
     * @param chatId     chatId==null ? global : personalization
     * @return 둘 중 하나의 가중치가 포함된 리스트
     */
    private List<BookSearchResponse> applyAdditionalScores(List<BookSearchResponse> candidates, Long chatId) {
        if (chatId != null) {
            return applyPersonalizationScores(candidates, chatId);
        }
        return applyGlobalFeedbackScores(candidates);
    }

    /**
     * 선호도 벡터 기반 점수 추가 (가중치 0.3)
     *
     * @param candidates 하이브리드 검색 결과
     * @param chatId     챗 아이디
     * @return 선호도가 반영된 리스트
     */
    private List<BookSearchResponse> applyPersonalizationScores(List<BookSearchResponse> candidates, Long chatId) {
        List<Long> candidateIds = candidates.stream().map(BookSearchResponse::getId).toList();
        Map<Long, Double> personalizationScores = userPreferenceService.getPersonalizationScores(candidateIds, chatId);

        return candidates.stream().map(book -> {
            Double pScore = personalizationScores.getOrDefault(book.getId(), 0.0);
            if (pScore > 0.0) {
                double finalScore = (book.getRrfScore() != null ? book.getRrfScore() : 0.0) + (pScore * 0.3);
                return book.withRrfScore(finalScore).withPersonalizationScore(pScore);
            }
            return book;
        }).toList();
    }

    /**
     * 글로벌 피드백 통계 기반 점수 가산 (가중치 0.5)
     *
     * @param candidates 하이브리드 검색 결과
     * @return 피드백 통계가 반영된 리스트
     */
    private List<BookSearchResponse> applyGlobalFeedbackScores(List<BookSearchResponse> candidates) {
        List<Long> bookIds = candidates.stream().map(BookSearchResponse::getId).toList();
        Map<Long, FeedbackStats> statsMap = telegramFeedbackClient.getBooksFeedbackStats(bookIds);

        return candidates.stream().map(book -> {
            FeedbackStats stats = statsMap.get(book.getId());
            if (stats != null && stats.hasMinimumCount(5)) {
                double feedbackBonus = 0.0;
                double ratio = stats.goodRatio();

                if (ratio >= 0.6) {
                    // 긍정 비율 60% 이상: 가산
                    feedbackBonus = stats.feedbackScore() * 0.5;
                } else if (ratio <= 0.4) {
                    // 긍정 비율 40% 이하: 감산
                    feedbackBonus = stats.feedbackScore() * 0.5;
                }

                double finalScore = (book.getRrfScore() != null ? book.getRrfScore() : 0.0) + feedbackBonus;
                return book.withRrfScore(finalScore);
            }
            return book;
        }).toList();
    }

    /**
     * 정렬, 임계값 필터링 및 Fallback 처리
     *
     * @param candidates 가중치가 부여된 도서 목록
     * @return 정렬 및 임계값 필터링 된 도서 리스트
     */

    private List<BookSearchResponse> filterAndLimitCandidates(List<BookSearchResponse> candidates) {
        // -------- rrf score가 높은 순으로 내림차순 정렬 ----------
        List<BookSearchResponse> rankedBooks = candidates.stream()
                .filter(book -> book.getRrfScore() != null)
                .sorted(Comparator.comparing(BookSearchResponse::getRrfScore).reversed())
                .toList();
        // ----------- rrf 점수 하한선 이상으로 추출 -----------
        List<BookSearchResponse> topBooks = rankedBooks.stream()
                .filter(book -> book.getRrfScore() >= rrfScoreThreshold)
                .limit(businessFilterK)
                .toList();
        // --------- 하한선 통과 결과가 없을 시 rrfScore가 높은 순서대로 5개만 추출하는 fallback ---------
        if (topBooks.isEmpty() && !rankedBooks.isEmpty()) {
            topBooks = rankedBooks.stream().limit(businessFilterK).toList();
            log.info("[RAG Top-K] 임계값 통과 결과가 없어 상위 {}권을 fallback으로 사용", topBooks.size());
        } else {
            log.info("[RAG Top-K] 임계값 통과: {}", topBooks.size());
        }

        return topBooks;
    }
}
