package com.nhnacademy.springailibrarycore.book.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.springailibrarycore.agent.BookSearchAgent;
import com.nhnacademy.springailibrarycore.book.domain.BookSearchCache;
import com.nhnacademy.springailibrarycore.book.domain.SearchType;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchPageResult;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResult;
import com.nhnacademy.springailibrarycore.book.dto.ai.BookRecommendation;
import com.nhnacademy.springailibrarycore.book.dto.ai.RecommendationResult;
import com.nhnacademy.springailibrarycore.book.repository.BookRepository;
import com.nhnacademy.springailibrarycore.book.repository.BookSearchCacheRepository;
import com.nhnacademy.springailibrarycore.book.repository.impl.search.KeywordBookSearchRepository;
import com.nhnacademy.springailibrarycore.book.repository.impl.search.VectorBookSearchRepository;
import com.nhnacademy.springailibrarycore.book.service.cache.RecommendationCacheCodec;
import com.nhnacademy.springailibrarycore.review.repository.ReviewRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * RAG 도서 검색 시 전체 아키텍처(Service -> Strategy -> Reranker -> Agent -> Cache)가
 * 유기적으로 통합되어 흐름대로 정상 작동하는지 검증하는 통합 테스트입니다.
 */
@SpringBootTest(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "spring.cache.type=none"
})
@EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        RedisAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration.class,
        RabbitAutoConfiguration.class
})
@ActiveProfiles("test")
class BookSearchFlowIntegrationTest {

    @Autowired
    private BookSearchAgent bookSearchAgent;

    // 데이터베이스 & 외부 저장소 Mocking
    @MockitoBean
    private KeywordBookSearchRepository keywordBookSearchRepository;

    @MockitoBean
    private VectorBookSearchRepository vectorBookSearchRepository;

    @MockitoBean
    private BookSearchCacheRepository bookSearchCacheRepository;

    @MockitoBean
    private BookRepository bookRepository;

    @MockitoBean
    private ReviewRepository reviewRepository;

    @MockitoBean
    private com.nhnacademy.springailibrarycore.review.service.ReviewService reviewService;

    // Redis Mocking
    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    // RabbitMQ Mocking
    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    // 임베딩 & Chat Model Mocking (컨텍스트 로딩 용도)
    @MockitoBean(name = "openAiEmbeddingModel")
    private EmbeddingModel openAiEmbeddingModel;

    @MockitoBean(name = "ollamaChatModel")
    private ChatModel ollamaChatModel;

    @MockitoBean(name = "googleGenAiChatModel")
    private ChatModel googleGenAiChatModel;

    // ChatClient 모킹용
    @Autowired
    @Qualifier("geminiChatClientBuilder")
    private ChatClient.Builder geminiChatClientBuilder;

    @Autowired
    @Qualifier("ollamaChatClientBuilder")
    private ChatClient.Builder ollamaChatClientBuilder;

    @Autowired
    @Qualifier("geminiChatClient")
    private ChatClient geminiChatClient;

    @Autowired
    private RecommendationCacheCodec cacheCodec;

    @Autowired
    private ObjectMapper objectMapper;

    @TestConfiguration
    static class TestConfig {
        @Bean(name = "geminiChatClient")
        public ChatClient geminiChatClient() {
            return Mockito.mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS);
        }

        @Bean(name = "geminiChatClientBuilder")
        public ChatClient.Builder geminiChatClientBuilder(@Qualifier("geminiChatClient") ChatClient geminiChatClient) {
            ChatClient.Builder builder = Mockito.mock(ChatClient.Builder.class);
            Mockito.when(builder.defaultSystem(anyString())).thenReturn(builder);
            Mockito.when(builder.build()).thenReturn(geminiChatClient);
            return builder;
        }

        @Bean(name = "ollamaChatClientBuilder")
        public ChatClient.Builder ollamaChatClientBuilder() {
            ChatClient.Builder builder = Mockito.mock(ChatClient.Builder.class);
            ChatClient chatClient = Mockito.mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS);
            Mockito.when(builder.defaultSystem(anyString())).thenReturn(builder);
            Mockito.when(builder.build()).thenReturn(chatClient);
            return builder;
        }

        @Bean
        public com.querydsl.jpa.impl.JPAQueryFactory jpaQueryFactory() {
            return Mockito.mock(com.querydsl.jpa.impl.JPAQueryFactory.class);
        }

        @Bean(name = "cacheRedisConnectionFactory")
        public org.springframework.data.redis.connection.RedisConnectionFactory cacheRedisConnectionFactory() {
            return Mockito.mock(org.springframework.data.redis.connection.RedisConnectionFactory.class);
        }

        @Bean
        @org.springframework.context.annotation.Primary
        public org.springframework.cache.CacheManager cacheManager() {
            return new org.springframework.cache.support.NoOpCacheManager();
        }
    }

    @BeforeEach
    void setUp() {
        // Embedding Model 모킹
        when(openAiEmbeddingModel.embed(anyString())).thenReturn(new float[1024]);

        // bookRepository 위임 모킹
        when(bookRepository.search(any(Pageable.class), any(BookSearchRequest.class)))
                .thenAnswer(invocation -> keywordBookSearchRepository.search(
                        invocation.getArgument(0),
                        invocation.getArgument(1)
                ));

        when(bookRepository.vectorSearch(any(Pageable.class), any(BookSearchRequest.class)))
                .thenAnswer(invocation -> vectorBookSearchRepository.search(
                        invocation.getArgument(0),
                        ((BookSearchRequest) invocation.getArgument(1)).vector()
                ));

        // StringRedisTemplate mock
        org.springframework.data.redis.core.ValueOperations valueOps = Mockito.mock(org.springframework.data.redis.core.ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);

        // ReviewService mock setup
        when(reviewService.getCachedSummary(any(Long.class)))
                .thenReturn(new com.nhnacademy.springailibrarycore.review.dto.ReviewSummaryResponse(
                        1L,
                        com.nhnacademy.springailibrarycore.review.domain.ReviewStatus.DONE,
                        "리뷰 요약입니다.",
                        null,
                        1L
                ));
    }

    @Test
    @DisplayName("RAG 캐시 HIT 시: Reranker 및 AI 추천 에이전트를 건너뛰고 캐시된 결과를 즉시 반환한다")
    void search_cacheHit_flow() {
        // given
        String keyword = "스프링 프레임워크";
        float[] dummyVector = new float[1024];
        Pageable pageable = PageRequest.of(0, 5);
        BookSearchRequest request = new BookSearchRequest(keyword, null, SearchType.RAG, dummyVector, false);

        // 캐시 데이터 모킹
        BookSearchResponse cachedBook = BookSearchResponse.builder()
                .id(100L)
                .title("캐시된 스프링 책")
                .authorName("토비")
                .bookContent("스프링 기초 지식")
                .aiComment("사전 캐싱된 완벽한 도서입니다.")
                .relevance(100)
                .build();
        List<BookSearchResponse> cachedList = List.of(cachedBook);

        BookSearchCache mockCache = BookSearchCache.create(
                keyword,
                dummyVector,
                cacheCodec.encode(cachedList),
                java.time.OffsetDateTime.now(Clock.systemUTC()),
                1800
        );

        when(bookSearchCacheRepository.findBestMatch(any(float[].class), any(Double.class)))
                .thenReturn(Optional.of(mockCache));

        // when
        BookSearchResult result = bookSearchAgent.searchBooks(pageable, request);

        // then
        assertThat(result.books()).hasSize(1);
        assertThat(result.books().getContent().get(0).getTitle()).isEqualTo("캐시된 스프링 책");
        assertThat(result.books().getContent().get(0).getAiComment()).isEqualTo("사전 캐싱된 완벽한 도서입니다.");

        // 캐시가 매치되었으므로 키워드 및 벡터 리포지터리 조회 쿼리는 실행되지 않아야 함
        verify(keywordBookSearchRepository, times(0)).search(any(Pageable.class), any(BookSearchRequest.class));
        verify(vectorBookSearchRepository, times(0)).search(any(Pageable.class), any(float[].class));
    }

    @Test
    @DisplayName("RAG 캐시 MISS 시: Hybrid 조회 -> RRF Rerank -> AI 추천 코멘트 부여 -> RDBMS 캐시 저장 흐름이 정상 동작한다")
    void search_cacheMiss_and_fullRAG_flow() {
        // given
        String keyword = "객체지향 설계";
        float[] dummyVector = new float[1024];
        Pageable pageable = PageRequest.of(0, 5);
        BookSearchRequest request = new BookSearchRequest(keyword, null, SearchType.RAG, dummyVector, false);

        // 1. 캐시 미스 모킹
        when(bookSearchCacheRepository.findBestMatch(any(float[].class), any(Double.class)))
                .thenReturn(Optional.empty());

        // 2. 키워드 검색 결과 모킹 (RRF용)
        BookSearchResponse keywordBook1 = BookSearchResponse.builder()
                .id(1L)
                .title("객체지향의 사실과 오해")
                .authorName("조영호")
                .bookContent("객체지향의 본질")
                .build();
        when(keywordBookSearchRepository.search(any(Pageable.class), any(BookSearchRequest.class)))
                .thenReturn(new BookSearchPageResult(List.of(keywordBook1), 1));

        // 3. 벡터 검색 결과 모킹 (RRF용)
        BookSearchResponse vectorBook1 = BookSearchResponse.builder()
                .id(1L)
                .title("객체지향의 사실과 오해")
                .authorName("조영호")
                .bookContent("객체지향의 본질")
                .similarity(0.92)
                .build();
        when(vectorBookSearchRepository.search(any(Pageable.class), any(float[].class)))
                .thenReturn(new BookSearchPageResult(List.of(vectorBook1), 1));

        // 4. AI 추천 에이전트 모킹
        BookRecommendation rec = new BookRecommendation(1L, 95, "객체지향의 기본 개념을 매우 쉽고 재미있게 풀어낸 명서입니다.");
        RecommendationResult mockAiResult = new RecommendationResult(List.of(rec));
        
        // Deep Stubbing을 이용해 한 줄로 모킹 수행
        when(geminiChatClient.prompt().user(anyString()).call().entity(RecommendationResult.class))
                .thenReturn(mockAiResult);

        // when
        BookSearchResult result = bookSearchAgent.searchBooks(pageable, request);

        // then
        assertThat(result.books()).hasSize(1);
        BookSearchResponse finalBook = result.books().getContent().get(0);
        assertThat(finalBook.getId()).isEqualTo(1L);
        assertThat(finalBook.getTitle()).isEqualTo("객체지향의 사실과 오해");
        // Reranker RRF 점수가 잘 들어가 있는지 확인
        assertThat(finalBook.getRrfScore()).isNotNull().isGreaterThan(0.0);
        // AI 추천 사유가 성공적으로 채워졌는지 확인
        assertThat(finalBook.getRelevance()).isEqualTo(95);
        assertThat(finalBook.getAiComment()).isEqualTo("객체지향의 기본 개념을 매우 쉽고 재미있게 풀어낸 명서입니다.");

        // 캐시 저장이 수행되었는지 확인
        verify(bookSearchCacheRepository, times(1)).save(any(BookSearchCache.class));
    }
}
