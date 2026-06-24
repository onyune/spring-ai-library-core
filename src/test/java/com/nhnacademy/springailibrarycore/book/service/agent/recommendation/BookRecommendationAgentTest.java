package com.nhnacademy.springailibrarycore.book.service.agent.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

class BookRecommendationAgentTest {

    private ChatClient chatClient;
    private ChatClient.Builder chatClientBuilder;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callResponseSpec;

    private BookRecommendationAgent agent;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        chatClientBuilder = mock(ChatClient.Builder.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callResponseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClientBuilder.defaultSystem(anyString())).thenReturn(chatClientBuilder);
        when(chatClientBuilder.build()).thenReturn(chatClient);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);

        agent = new BookRecommendationAgent(chatClientBuilder);
    }

    @Test
    @DisplayName("AI 응답을 받아 도서 정보(aiComment, relevance)를 성공적으로 채우고 순서를 유지한다")
    void enrich_success() {
        // given
        BookSearchResponse book1 = BookSearchResponse.builder()
                .id(1L)
                .title("토비의 스프링")
                .authorName("이일민")
                .publisherName("에이콘")
                .price(BigDecimal.valueOf(35000))
                .bookContent("스프링 기본서")
                .build();

        BookSearchResponse book2 = BookSearchResponse.builder()
                .id(2L)
                .title("오브젝트")
                .authorName("조영호")
                .publisherName("위키북스")
                .price(BigDecimal.valueOf(30000))
                .bookContent("객체지향 설계")
                .build();

        List<BookSearchResponse> books = List.of(book1, book2);

        BookRecommendationAgent.BookRecommendation rec1 = new BookRecommendationAgent.BookRecommendation(1L, 95, "스프링 핵심 원리를 깊이 있게 설명하는 책입니다.");
        BookRecommendationAgent.BookRecommendation rec2 = new BookRecommendationAgent.BookRecommendation(2L, 88, "객체지향 설계를 마스터하기에 아주 좋은 책입니다.");
        BookRecommendationAgent.RecommendationResult mockResult = new BookRecommendationAgent.RecommendationResult(List.of(rec1, rec2));

        when(callResponseSpec.entity(BookRecommendationAgent.RecommendationResult.class)).thenReturn(mockResult);

        // when
        List<BookSearchResponse> enriched = agent.enrich("스프링 객체지향 관련 도서 추천해줘", books);

        // then
        assertThat(enriched).hasSize(2);
        assertThat(enriched.get(0).getId()).isEqualTo(1L);
        assertThat(enriched.get(0).getRelevance()).isEqualTo(95);
        assertThat(enriched.get(0).getAiComment()).isEqualTo("스프링 핵심 원리를 깊이 있게 설명하는 책입니다.");

        assertThat(enriched.get(1).getId()).isEqualTo(2L);
        assertThat(enriched.get(1).getRelevance()).isEqualTo(88);
        assertThat(enriched.get(1).getAiComment()).isEqualTo("객체지향 설계를 마스터하기에 아주 좋은 책입니다.");
    }

    @Test
    @DisplayName("AI 응답이 null인 경우 원본 도서 목록을 그대로 반환한다")
    void enrich_nullResponse() {
        // given
        BookSearchResponse book = BookSearchResponse.builder()
                .id(1L)
                .title("토비의 스프링")
                .build();
        List<BookSearchResponse> books = List.of(book);

        when(callResponseSpec.entity(BookRecommendationAgent.RecommendationResult.class)).thenReturn(null);

        // when
        List<BookSearchResponse> enriched = agent.enrich("검색", books);

        // then
        assertThat(enriched).hasSize(1);
        assertThat(enriched.get(0).getAiComment()).isNull();
        assertThat(enriched.get(0).getRelevance()).isNull();
    }

    @Test
    @DisplayName("AI 호출 중 예외가 발생하면 예외를 잡아 로그를 남기고 원본 목록을 그대로 반환한다")
    void enrich_exceptionFallback() {
        // given
        BookSearchResponse book = BookSearchResponse.builder()
                .id(1L)
                .title("토비의 스프링")
                .build();
        List<BookSearchResponse> books = List.of(book);

        when(callResponseSpec.entity(BookRecommendationAgent.RecommendationResult.class))
                .thenThrow(new RuntimeException("AI API Timeout"));

        // when
        List<BookSearchResponse> enriched = agent.enrich("검색", books);

        // then
        assertThat(enriched).hasSize(1);
        assertThat(enriched.get(0).getAiComment()).isNull();
        assertThat(enriched.get(0).getRelevance()).isNull();
    }
}
