package com.nhnacademy.springailibrarycore.book.strategy.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.springailibrarycore.book.service.agent.recommendation.BookRecommendationAgent;
import com.nhnacademy.springailibrarycore.book.domain.SearchType;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.service.agent.embedding.EmbeddingSubAgent;
import com.nhnacademy.springailibrarycore.book.strategy.SearchStrategy;
import com.nhnacademy.springailibrarycore.book.strategy.support.RrfBookReranker;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * RAG 프롬프트에 전달할 도서 후보를 선별하는 검색 전략입니다.
 *
 * 하이브리드 검색으로 Retrieval 후보를 확보하고, RRF 임계값 필터링과 정렬을 거쳐
 * 상위 Rerank K개의 도서만 반환합니다.
 */
@Component
@Slf4j
public class RagSearchStrategy implements SearchStrategy {
    private final RrfBookReranker rrfBookReranker;
    private final EmbeddingSubAgent embeddingSubAgent;
    private final RedisVectorStore redisVectorStore;
    private final ObjectMapper objectMapper;
    private final BookRecommendationAgent bookRecommendationAgent;
    private static final double SIMILARITY_THRESHOLD = 0.85;

    public RagSearchStrategy(
            RrfBookReranker rrfBookReranker,
            EmbeddingSubAgent embeddingSubAgent,
            @Qualifier("redisVectorStore") RedisVectorStore redisVectorStore,
            ObjectMapper objectMapper,
            BookRecommendationAgent bookRecommendationAgent
    ) {
        this.rrfBookReranker = rrfBookReranker;
        this.embeddingSubAgent = embeddingSubAgent;
        this.redisVectorStore = redisVectorStore;
        this.objectMapper = objectMapper;
        this.bookRecommendationAgent = bookRecommendationAgent;
    }

    @Override
    public SearchType supports() {
        return SearchType.RAG;
    }

    @Override
    public Page<BookSearchResponse> search(
            Pageable pageable,
            BookSearchRequest request
    ) {
        String normalizedQuestion = request.keyword().trim();
        float[] questionVector = request.vector() != null
                ? request.vector()
                : embeddingSubAgent.getEmbedding(normalizedQuestion);

        // ---------- Redis 벡터 캐시 조회 ----------
        SearchRequest searchRequest = SearchRequest.builder()
                .query(normalizedQuestion)
                .topK(pageable.getPageSize())
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build();
        List<Document> cachedDocuments = redisVectorStore.similaritySearch(searchRequest);

        if (cachedDocuments != null && !cachedDocuments.isEmpty()) {
            log.info("[VectorCache] Cache HIT - {}건 반환", cachedDocuments.size());
            // Document 1개에 응답 리스트 전체가 JSON으로 직렬화되어 있으므로 flatMap으로 평탄화
            List<BookSearchResponse> cacheResults = cachedDocuments.stream()
                    .flatMap(doc -> toSearchResponse(doc).stream())
                    .distinct()
                    .toList();

            if (!cacheResults.isEmpty()) {
                return new PageImpl<>(cacheResults, pageable, cacheResults.size());
            }
            log.warn("[VectorCache] Cache HIT이지만 역직렬화 결과가 비어 있어 재검색합니다.");
        }

        // ---------- 캐시 MISS → Hybrid Retrieval + RRF Rerank ----------
        log.info("[VectorCache] Cache MISS - Hybrid 검색 후 Redis 캐시 저장");
        BookSearchRequest vectorizedRequest = new BookSearchRequest(
                normalizedQuestion,
                request.isbn(),
                request.searchType(),
                questionVector,
                request.warmUp()
        );
        // --------------- RRF Rerank: 상위 5권 추출 ---------------
        List<BookSearchResponse> topBooks = rrfBookReranker.reranker(vectorizedRequest);

        if (topBooks.isEmpty()) {
            log.info("[RAG] 검색 결과 없음 - 빈 페이지 반환");
            return Page.empty(pageable);
        }

        // ---------- AI 추천 사유 부여 ----------
        List<BookSearchResponse> enrichedBooks = bookRecommendationAgent.enrich(normalizedQuestion, topBooks);

        // ---------- 결과를 Redis 벡터 스토어에 저장 ----------
        Document cacheDocument = toDocument(enrichedBooks, normalizedQuestion);
        if (cacheDocument != null) {
            try {
                redisVectorStore.add(List.of(cacheDocument));
                log.info("[VectorCache] {}권 Redis 캐시 저장 완료", enrichedBooks.size());
            } catch (Exception e) {
                log.warn("[VectorCache] Redis 캐시 저장 실패 (검색 결과는 정상 반환)", e);
            }
        }

        return new PageImpl<>(enrichedBooks, pageable, enrichedBooks.size());
    }

    /**
     * DTO → Spring AI Document 변환 (Redis 저장용)
     */
    private Document toDocument(List<BookSearchResponse> response, String question) {

        try{
            String jsonResponses = objectMapper.writeValueAsString(response);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("cached_responses", jsonResponses);
            return new Document(question, metadata);
        }catch (JsonProcessingException e){
            log.error("[RAG Cache] 도서 리스트 캐시 직렬화 실패", e);
            return null;
        }
    }

    /**
     * Spring AI Document → DTO 변환 (캐시 복원용)
     */
    private List<BookSearchResponse> toSearchResponse(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        String jsonResponses = (String) metadata.get("cached_responses");

        if(jsonResponses == null || jsonResponses.isBlank()){
            return List.of();
        }
        try{
            return objectMapper.readValue(jsonResponses, new TypeReference<List<BookSearchResponse>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("[RAG Cache] 도서 리스트 캐시 역직렬화 실패",e);
            return List.of();
        }
    }
}
