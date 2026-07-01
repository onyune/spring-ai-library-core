package com.nhnacademy.springailibrarycore.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.springailibrarycore.review.domain.Review;
import com.nhnacademy.springailibrarycore.review.domain.ReviewStatus;
import com.nhnacademy.springailibrarycore.review.dto.ReviewSummaryResponse;
import com.nhnacademy.springailibrarycore.review.repository.ReviewRepository;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 리뷰 요약 Agent들 조율
 * ReviewMapAgent와 ReviewReduceAgent들을 적절히 조율
 * 캐시된 리뷰와 마지막 요약 리뷰아이디 이후의 리뷰들을 비동기 병렬 요약하여 합산 후 리턴
 */
@Service
@Slf4j
public class ReviewCoordinator {
    private final ReviewRepository reviewRepository;
    private final ReviewMapAgent mapAgent;
    private final ReviewReduceAgent reduceAgent;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final Executor taskExecutor;

    public ReviewCoordinator(
            ReviewRepository reviewRepository,
            ReviewMapAgent mapAgent,
            ReviewReduceAgent reduceAgent,
            ObjectMapper objectMapper,
            StringRedisTemplate redisTemplate,
            @Qualifier("applicationTaskExecutor") Executor taskExecutor
    ) {
        this.reviewRepository = reviewRepository;
        this.mapAgent = mapAgent;
        this.reduceAgent = reduceAgent;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.taskExecutor = taskExecutor;
    }

    public ReviewSummaryResponse getOrGenerateSummary(Long bookId) throws JsonProcessingException {
        log.info("[ReviewCoordinator] 리뷰 요약 조회 및 생성 요청 -> bookId: {}", bookId);
        String cacheKey = "review:summary:" + bookId;

        // Redis 캐시 확인
        String strResponse = redisTemplate.opsForValue().get(cacheKey);
        ReviewSummaryResponse cachedResponse = null;
        if (strResponse != null) {
            cachedResponse = objectMapper.readValue(strResponse, ReviewSummaryResponse.class);
        }

        return generateSummary(bookId, cachedResponse, cacheKey);
    }

    /**
     * 최초 요약과 증분 요약 프로세스를 단 하나로 통합
     */
    private ReviewSummaryResponse generateSummary(Long bookId, ReviewSummaryResponse cachedResponse, String cacheKey) throws JsonProcessingException {
        List<Review> targetReviews;
        Long lastProcessedId = (cachedResponse != null) ? cachedResponse.lastProcessReviewId() : null;

        if (lastProcessedId == null) {
            // [최초 요약] 전체 최신 리뷰 최대 20개 가져오기
            targetReviews = reviewRepository.findTop20ByBookIdOrderByCreatedAtDesc(bookId);
            log.info("[ReviewCoordinator] 최초 요약 시작 -> bookId: {}, 대상 리뷰: {}개", bookId, targetReviews.size());
        } else {
            // [증분 요약] lastProcessedId 이후의 신규 리뷰 가져오기
            targetReviews = reviewRepository.findByBookIdAndIdGreaterThanOrderByIdDesc(bookId, lastProcessedId);
            log.info("[ReviewCoordinator] 증분 요약 시작 -> bookId: {}, 신규 리뷰: {}개", bookId, targetReviews.size());
        }

        if (targetReviews.isEmpty()) {
            if (cachedResponse != null) {
                return cachedResponse; // 신규 리뷰 없으면 기존 캐시 반환
            }
            return new ReviewSummaryResponse(bookId, ReviewStatus.DONE, "아직 작성된 독자 리뷰가 없습니다.", null, null);
        }

        // 가장 최신 리뷰 ID 결정
        Long nextLatestReviewId = targetReviews.stream().mapToLong(Review::getId).max().orElse(lastProcessedId != null ? lastProcessedId : 0L);

        // 5개씩 청크 분할 및 병렬 Map 요약 수행 (CompletableFuture)
        List<List<Review>> chunks = partition(targetReviews, 5);
        List<String> partialSummaries = summarizeChunksInParallel(chunks);
        String combinedSummaries = String.join("\n\n---\n\n", partialSummaries);

        // Reduce 단계 수행 (기존 캐시 요약본이 존재한다면 융합 처리)
        String finalReportInput = combinedSummaries;
        if (cachedResponse != null && cachedResponse.summaryText() != null) {
            finalReportInput = String.format(
                    "--- [기존 요약 보고서] ---\n%s\n\n--- [추가된 신규 독자 리뷰 요약본] ---\n%s",
                    cachedResponse.summaryText(),
                    combinedSummaries
            );
        }

        String finalReport = reduceAgent.reduceSummaries(finalReportInput);

        // Redis 캐싱
        ReviewSummaryResponse response = new ReviewSummaryResponse(bookId, ReviewStatus.DONE, finalReport, null, nextLatestReviewId);
        redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), Duration.ofDays(1));
        log.info("[ReviewCoordinator] 요약 완료 및 캐시 업데이트 -> bookId: {}", bookId);

        return response;
    }

    /**
     * 비동기 병렬 Map 요약 실행 (에러 처리 포함)
     */
    private List<String> summarizeChunksInParallel(List<List<Review>> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }

        List<CompletableFuture<String>> futures = chunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(() -> {
                    String chunkText = buildChunkText(chunk);
                    return mapAgent.summarizeChunk(chunkText);
                }, taskExecutor).exceptionally(ex -> {
                    log.error("[ReviewCoordinator] 개별 청크 요약 실패 - 우회 처리 진행", ex);
                    return "[일부 독자 리뷰 요약에 실패했습니다. (AI 호출 지연 또는 유해 내용 필터링)]";
                }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    private List<List<Review>> partition(List<Review> list, int size) {
        List<List<Review>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    private String buildChunkText(List<Review> chunk) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunk.size(); i++) {
            Review r = chunk.get(i);
            sb.append(String.format("리뷰 %d (평점 %d): %s\n", (i + 1), r.getRating(), r.getContent()));
        }
        return sb.toString();
    }
}
