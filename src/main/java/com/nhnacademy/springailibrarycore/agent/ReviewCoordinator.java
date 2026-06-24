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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 리뷰 요약 Agent들 조율
 * ReviewMapAgent와 ReviewReduceAgent들을 적절히 조율
 * 캐시된 리뷰와
 * 캐시된 리뷰에 있는 마지막 요약 리뷰아이디 이후의 리뷰들을 요약하여
 * 합산 후 리턴
 *
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewCoordinator {
    private final ReviewRepository reviewRepository;
    private final ReviewMapAgent mapAgent;
    private final ReviewReduceAgent reduceAgent;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public ReviewSummaryResponse getOrGenerateSummary(Long bookId) throws JsonProcessingException {
        log.info("[ReviewCoordinator] 증분 요약 조회 및 생성 요청 -> bookId: {}", bookId);
        String cacheKey = "review:summary:" + bookId;

        //요약된 정보 가져오기
        String strResponse = redisTemplate.opsForValue().get(cacheKey);

        if(strResponse != null) {
            ReviewSummaryResponse cachedResponse = objectMapper.readValue(strResponse, ReviewSummaryResponse.class);
            Long lastReviewId = cachedResponse.lastProcessReviewId();

            if(lastReviewId != null) {
                log.info("[ReviewCoordinator] 기존 캐시 발견. 중분 요약 시작 -> bookId: {} ", bookId);
                return generateIncrementalSummary(bookId, cachedResponse, lastReviewId, cacheKey);
            }
        }

        //캐시 없는 경우: 최초 요약 진행
        log.info("[ReviewCoordinator] 기존 캐시 없음. 최초 요약 진행 -> bookId: {}", bookId);
        return generateInitialSummary(bookId, cacheKey);
    }

    private ReviewSummaryResponse generateInitialSummary(Long bookId, String cacheKey) throws JsonProcessingException {
        List<Review> reviews = reviewRepository.findTop20ByBookIdOrderByCreatedAtDesc(bookId);
        if (reviews.isEmpty()) {
            return new ReviewSummaryResponse(bookId, ReviewStatus.DONE, "아직 작성된 독자 리뷰가 없습니다.", null, null);
        }

        Long latestReviewId = reviews.stream().mapToLong(Review::getId).max().orElse(0L);

        List<List<Review>> chunks = partition(reviews, 5);
        List<String> partialSummaries = new ArrayList<>();
        for (List<Review> chunk : chunks) {
            partialSummaries.add(mapAgent.summarizeChunk(buildChunkText(chunk)));
        }
        String finalReport = reduceAgent.reduceSummaries(String.join("\n\n---\n\n", partialSummaries));

        ReviewSummaryResponse response = new ReviewSummaryResponse(bookId, ReviewStatus.DONE, finalReport, null, latestReviewId);
        redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(response), Duration.ofDays(1));

        return response;
    }

    private ReviewSummaryResponse generateIncrementalSummary(Long bookId, ReviewSummaryResponse cachedResponse, Long lastReviewId, String cacheKey) throws JsonProcessingException {
        String oldSummaryText = cachedResponse.summaryText();

        List<Review> newReviews = reviewRepository.findByBookIdAndIdGreaterThanOrderByIdDesc(bookId, lastReviewId);
        Long nextLatestReviewId = newReviews.stream().mapToLong(Review::getId).max().orElse(lastReviewId);

        List<List<Review>> chunks = partition(newReviews, 5);
        List<String> newPartialSummaries = new ArrayList<>();
        for (List<Review> chunk : chunks) {
            newPartialSummaries.add(mapAgent.summarizeChunk(buildChunkText(chunk)));
        }

        String combinedNewSummaries = String.join("\n\n---\n\n", newPartialSummaries);

        String fusionText = String.format(
                "--- [기존 요약 보고서] ---%n%s%n%n--- [추가된 신규 독자 리뷰 요약본] ---%n%s",
                oldSummaryText,
                combinedNewSummaries
        );

        String updatedFinalReport = reduceAgent.reduceSummaries(fusionText);

        ReviewSummaryResponse updatedResponse = new ReviewSummaryResponse(bookId, ReviewStatus.DONE, updatedFinalReport, null, nextLatestReviewId);
        redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(updatedResponse), Duration.ofDays(1));
        log.info("[ReviewCoordinator] 증분 요약 완료 및 캐시 업데이트 -> bookId: {}", bookId);

        return updatedResponse;
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
