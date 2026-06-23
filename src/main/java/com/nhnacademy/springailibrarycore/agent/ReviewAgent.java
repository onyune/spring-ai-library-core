package com.nhnacademy.springailibrarycore.agent;

import com.nhnacademy.springailibrarycore.review.domain.Review;
import com.nhnacademy.springailibrarycore.review.repository.ReviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * 리뷰 분석 및 요약 담당 Agent
 */
@Component
@Slf4j
public class ReviewAgent {

    private final ReviewRepository reviewRepository;
    private final ChatClient chatClient;

    public ReviewAgent(ReviewRepository reviewRepository,
                       @Qualifier("geminiChatClientBuilder") ChatClient.Builder chatClientBuilder
    ) {
        this.reviewRepository = reviewRepository;
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 특정 도서의 리뷰들을 Map-Reduce 기법으로 종합 요약하여 반환
     *
     * @param bookId 도서 ID
     * @return 요약된 마크다운 형태의 리뷰 리포트 텍스트
     */
    public String summarizeReviews(Long bookId) {
        log.info("[ReviewAgent] 도서 ID {}에 대한 리뷰 요약 시작", bookId);

        // 최근 20개 리뷰 조회
        List<Review> reviews = reviewRepository.findTop20ByBookIdOrderByCreatedAtDesc(bookId);
        if (reviews.isEmpty()) {
            return "아직 작성된 독자 리뷰가 없습니다.";
        }

        // 리뷰를 5개씩 청크 분할
        int chunkSize = 5;
        List<List<Review>> chunks = partition(reviews, chunkSize);
        log.info("[ReviewAgent] 총 {}개의 리뷰를 {}개의 청크로 분할하여 Map 요약 시작", reviews.size(), chunks.size());

        // Map 단계: 청크별 부분 요약
        List<String> partialSummaries = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            List<Review> chunk = chunks.get(i);
            String chunkText = buildChunkText(chunk);

            String mapPrompt = """
                제시된 도서 리뷰 목록의 핵심 내용을 요약하여, 긍정적인 평과 부정적인 평을 각각 2~3줄 내외로 요점만 정리하세요.
                리뷰 외의 잡담이나 메타 발언은 금지합니다.
                """;

            String partialSummary = chatClient.prompt()
                .system(mapPrompt)
                .user(chunkText)
                .call()
                .content();

            partialSummaries.add(partialSummary);
            log.info("[ReviewAgent] Map 단계 진행 중: {}/{}", (i + 1), chunks.size());
        }

        // Reduce 단계: 요약본들을 한 군데 융합하여 최종 리포트로 정제
        log.info("[ReviewAgent] {}개의 부분 요약본을 하나로 종합하는 Reduce 단계 시작", partialSummaries.size());
        String combinedSummaries = String.join("\n\n---\n\n", partialSummaries);

        String reducePrompt = """
            당신은 도서 평평 분석 전문가입니다. 분할되어 요약된 아래의 부분 리뷰 보고서들을 분석하여,
            이 도서에 대한 전반적인 장점(Good Points)과 단점(Bad Points)을 정돈하고, 최종 종합 의견을 마크다운 문법으로 깔끔하게 작성해 주세요.
            """;

        return chatClient.prompt()
            .system(reducePrompt)
            .user(combinedSummaries)
            .call()
            .content();
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
            sb.append(String.format("리뷰 %d (평점: %d점): %s\n", (i + 1), r.getRating(), r.getContent()));
        }
        return sb.toString();
    }
}
