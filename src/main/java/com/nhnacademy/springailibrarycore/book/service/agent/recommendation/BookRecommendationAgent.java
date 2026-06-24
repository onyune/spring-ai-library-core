package com.nhnacademy.springailibrarycore.book.service.agent.recommendation;

import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.dto.ai.BookRecommendation;
import com.nhnacademy.springailibrarycore.book.dto.ai.RecommendationResult;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * RRF Rerank로 선별된 상위 도서 목록에 AI 추천 사유(aiComment)와
 * 연관성 점수(relevance)를 부여하는 에이전트입니다.
 *
 * Gemini ChatClient를 사용하며, AI 호출 실패 시 원본 목록을 그대로 반환합니다(Fallback).
 */
@Slf4j
@Service
public class BookRecommendationAgent {

    /** AI에 전달할 bookContent 최대 길이 (토큰 절약) */
    private static final int MAX_CONTENT_LENGTH = 200;

    private final ChatClient chatClient;

    public BookRecommendationAgent(
            @Qualifier("geminiChatClientBuilder") ChatClient.Builder chatClientBuilder
    ) {
        String systemPrompt = """
                너는 도서관 큐레이터야.
                사용자의 검색 질문에 대해 각 도서가 왜 추천되는지 한국어로 2~3문장으로 설명해.
                relevance는 0~100 사이의 정수로, 질문과의 연관성 점수야.
                
                [규칙]
                - bookId는 반드시 입력으로 받은 값을 그대로 사용해.
                - recommendations 배열에 입력된 모든 도서에 대해 빠짐없이 응답해.
                - aiComment는 반드시 한국어로 작성해.
                - relevance가 낮아도 이유는 성실히 작성해.
                """;
        this.chatClient = chatClientBuilder
                .defaultSystem(systemPrompt)
                .build();
    }

    /**
     * RRF Rerank 완료된 도서 목록에 AI 추천 사유를 부여합니다.
     *
     * @param question 사용자의 원본 검색 질문
     * @param books    RRF 점수 기준 정렬된 상위 도서 목록
     * @return aiComment / relevance 가 채워진 도서 목록 (순서 유지)
     */
    public List<BookSearchResponse> enrich(String question, List<BookSearchResponse> books) {
        if (books.isEmpty()) {
            return List.of();
        }

        String userPrompt = buildUserPrompt(question, books);

        try {
            RecommendationResult result = chatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .entity(RecommendationResult.class);

            if (result == null || result.recommendations() == null || result.recommendations().isEmpty()) {
                log.warn("[BookRecommendationAgent] AI 응답이 비어있어 원본 목록을 반환합니다.");
                return books;
            }

            // bookId → BookRecommendation Map
            Map<Long, BookRecommendation> recommendationMap = result.recommendations().stream()
                    .filter(r -> r.bookId() != null)
                    .collect(Collectors.toMap(
                            BookRecommendation::bookId,
                            Function.identity(),
                            (a, b) -> a // 중복 bookId는 첫 번째 우선
                    ));

            // 원본 순서(RRF 순위) 유지하면서 aiComment / relevance 주입
            List<BookSearchResponse> enriched = books.stream()
                    .map(book -> {
                        BookRecommendation rec = recommendationMap.get(book.getId());
                        if (rec == null) {
                            log.debug("[BookRecommendationAgent] bookId={} 에 대한 AI 응답 없음, 원본 유지", book.getId());
                            return book;
                        }
                        return book.withAiComment(rec.relevance(), rec.aiComment());
                    })
                    .toList();

            log.info("[BookRecommendationAgent] {}권 AI 추천 사유 부여 완료", enriched.size());
            return enriched;

        } catch (Exception e) {
            log.warn("[BookRecommendationAgent] AI 호출 실패 - 원본 목록 반환 (Fallback)", e);
            return books;
        }
    }

    /**
     * AI에 전달할 User 프롬프트를 구성합니다.
     * bookContent가 없는 도서는 제목/저자/출판사만으로 구성합니다.
     */
    private String buildUserPrompt(String question, List<BookSearchResponse> books) {
        StringBuilder sb = new StringBuilder();
        sb.append("질문: \"").append(question).append("\"\n\n");
        sb.append("도서 목록:\n");

        for (int i = 0; i < books.size(); i++) {
            BookSearchResponse book = books.get(i);
            sb.append(i + 1).append(". ")
              .append("[bookId=").append(book.getId()).append("] ")
              .append("제목: ").append(book.getTitle()).append(" / ")
              .append("저자: ").append(nullSafe(book.getAuthorName())).append(" / ")
              .append("출판사: ").append(nullSafe(book.getPublisherName()));

            if (book.getBookContent() != null && !book.getBookContent().isBlank()) {
                String truncated = book.getBookContent().length() > MAX_CONTENT_LENGTH
                        ? book.getBookContent().substring(0, MAX_CONTENT_LENGTH) + "..."
                        : book.getBookContent();
                sb.append("\n   소개: ").append(truncated);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String nullSafe(String value) {
        return value != null ? value : "-";
    }

}
