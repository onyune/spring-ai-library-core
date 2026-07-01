package com.nhnacademy.springailibrarycore.book.service.agent.recommendation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.dto.ai.BookRecommendation;
import com.nhnacademy.springailibrarycore.review.service.ReviewService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * RRF Rerank로 선별된 상위 도서 목록에 AI 추천 사유(aiComment)와 연관성 점수(relevance)를 부여하는 에이전트입니다.
 *
 * Gemini ChatClient를 사용하며, AI 호출 실패 시 원본 목록을 그대로 반환합니다(Fallback).
 */
@Slf4j
@Service
public class BookRecommendationAgent {

    /**
     * AI에 전달할 bookContent 최대 길이 (토큰 절약)
     */
    private static final int MAX_CONTENT_LENGTH = 200;

    private final ChatClient chatClient;
    private final ReviewService reviewService;
    private final ObjectMapper objectMapper;
    private final GoogleGenAiChatOptions jsonOptions;

    public BookRecommendationAgent(
            @Qualifier("geminiJsonChatClientBuilder") ChatClient.Builder chatClientBuilder,
            ReviewService reviewService,
            ObjectMapper objectMapper
    ) { // 연관성 점수의 조건 (구간별 점수화)
        String systemPrompt = """
                너는 최고의 전문성을 갖춘 수석 도서관 큐레이터다.
                아래에 제공된 후보 도서(최대 20권)와 사용자의 리뷰 요약을 꼼꼼히 분석한 뒤,
                사용자의 검색 질문 의도(난이도, 기술 스택, 주제 등)와 가장 완벽하게 부합하는 최상위 도서(최소 1권 ~ 최대 5권)만 네가 직접 선별해.
               
                
                [선별 및 평가 기준표]
                선별한 도서에 대해 0~100점 사이의 연관성 점수(relevance)를 엄격하게 부여해.
                - 90~100점: 질문의 핵심 주제, 기술 스택, 난이도와 완벽히 일치하는 최고의 필수 권장 도서.
                - 80~89점: 핵심 주제와 부합하나, 특정 세부 조건(프레임워크 버전, 깊이 등)에서 약간 차이가 있는 훌륭한 대체/보완 도서.
                - 60~79점: 관련 주제를 일부 다루고 있으나, 질문의 핵심 의도와는 포커스가 다르거나 부분적인 정보만 제공하는 도서.
                - 40~59점: 큰 카테고리만 겹칠 뿐, 사용자가 실제로 원하는 구체적인 지식이나 난이도와는 꽤 거리가 먼 도서.
                - 0~39점: 사용자의 질문과 전혀 무관하거나, 단순 키워드만 우연히 겹쳐서 추천하기에 매우 부적합한 도서.
                
                [응답 규칙]
                1. 전달받은 도서 목록 중 점수가 가장 높은 상위 도서(최대 5권)에 대해서만 응답을 생성하고, 나머지 탈락한 도서는 절대 응답에 포함하지 마.
                2. 선택된 도서에 대해, 왜 이 책이 사용자에게 꼭 필요한지 한국어로 2~3문장의 구체적인 추천 사유(aiComment)를 작성해.
                3. bookId는 반드시 입력으로 받은 값을 그대로 사용해.
                """;
        this.reviewService = reviewService;
        this.objectMapper = objectMapper;
        this.jsonOptions = buildJsonOptions();
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
            String jsonResponse = chatClient.prompt()
                    .user(userPrompt)
                    .options(jsonOptions)
                    .call()
                    .content();

            log.info("[BookRecommendationAgent] AI 원본 응답:\n{}", jsonResponse);

            List<BookRecommendation> recommendations = objectMapper.readValue(jsonResponse,
                    new TypeReference<List<BookRecommendation>>() {
                    });
            
            log.info("[BookRecommendationAgent] 파싱된 추천 결과 ({}건): {}", 
                     recommendations.size(), recommendations);

            if (recommendations == null || recommendations.isEmpty()) {
                log.warn("[BookRecommendationAgent] AI 응답이 비어있어 원본 목록 상위 5권을 반환합니다.");
                return books.stream().limit(5).toList();
            }

            return mergeRecommendations(books, recommendations);

        } catch (Exception e) {
            log.warn("[BookRecommendationAgent] AI 호출 실패 - 원본 목록 반환 (Fallback)", e);
            return books;
        }
    }

    /**
     * AI에 전달할 User 프롬프트를 구성합니다. bookContent가 없는 도서는 제목/저자/출판사만으로 구성합니다.
     */
    private String buildUserPrompt(String question, List<BookSearchResponse> books) {
        StringBuilder sb = new StringBuilder();
        sb.append("질문: \"").append(question).append("\"\n\n");
        sb.append("도서 목록:\n");

        for (int i = 0; i < books.size(); i++) {
            BookSearchResponse book = books.get(i);
            var cachedSummary = reviewService.getCachedSummary(book.getId());
            String summaryReview = (cachedSummary != null) ? nullSafe(cachedSummary.summaryText()) : "-";

            sb.append(i + 1).append(". ")
                    .append("[bookId=").append(book.getId()).append("] ")
                    .append("제목: ").append(book.getTitle()).append(" / ")
                    .append("저자: ").append(nullSafe(book.getAuthorName())).append(" / ")
                    .append("출판사: ").append(nullSafe(book.getPublisherName()))
                    .append("리뷰요약: ").append(summaryReview);

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

    private List<BookSearchResponse> mergeRecommendations(List<BookSearchResponse> books, List<BookRecommendation> recommendations) {
        // bookId → BookRecommendation Map
        Map<Long, BookRecommendation> recommendationMap = recommendations.stream()
                .filter(r -> r.bookId() != null)
                .collect(Collectors.toMap(
                        BookRecommendation::bookId,
                        Function.identity(),
                        (a, b) -> a // 중복 bookId는 첫 번째 우선
                ));

        // AI가 선택한 도서 (AI 연관성 점수 기준 내림차순 정렬)
        List<BookSearchResponse> aiSelectedBooks = books.stream()
                .filter(book -> recommendationMap.containsKey(book.getId()))
                .map(book -> {
                    BookRecommendation rec = recommendationMap.get(book.getId());
                    return book.withAiComment(rec.relevance(), rec.aiComment());
                })
                .sorted((b1, b2) -> Integer.compare(
                        b2.getRelevance() != null ? b2.getRelevance() : 0,
                        b1.getRelevance() != null ? b1.getRelevance() : 0
                ))
                .toList();

        // AI가 선택하지 않은 나머지 도서 (원본 Cohere 순위 유지, 코멘트 없음)
        List<BookSearchResponse> unselectedBooks = books.stream()
                .filter(book -> !recommendationMap.containsKey(book.getId()))
                .toList();

        // AI 추천 도서 + 일반 추천 도서 병합
        List<BookSearchResponse> finalResult = Stream.concat(
                aiSelectedBooks.stream(),
                unselectedBooks.stream()
        ).toList();

        log.info("[BookRecommendationAgent] AI 추천 {}권 + 일반 추천 {}권 = 총 {}권 반환",
                aiSelectedBooks.size(), unselectedBooks.size(), finalResult.size());

        return finalResult;
    }

    private GoogleGenAiChatOptions buildJsonOptions(){
        String schemaJson = """
            {
              "type": "ARRAY",
              "items": {
                "type": "OBJECT",
                "properties": {
                  "bookId":    { "type": "NUMBER" },
                  "relevance": { "type": "INTEGER" },
                  "aiComment": { "type": "STRING" }
                },
                "required": ["bookId", "relevance", "aiComment"]
              }
            }
            """;
        return GoogleGenAiChatOptions.builder()
                .responseMimeType("application/json")
                .responseSchema(schemaJson)
                .build();
    }

}
