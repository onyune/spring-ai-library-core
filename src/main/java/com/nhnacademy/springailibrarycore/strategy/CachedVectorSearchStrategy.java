package com.nhnacademy.springailibrarycore.strategy;

import com.nhnacademy.springailibrarycore.domain.SearchType;
import com.nhnacademy.springailibrarycore.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.repository.BookRepository;
import com.nhnacademy.springailibrarycore.service.embedding.EmbeddingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Redis Vector Store를 캐시 레이어로 사용하고,
 * 캐시 미스 시 PostgreSQL(pgvector)을 조회하는 캐시형 벡터 검색 전략입니다.
 *
 * <ul>
 *   <li>Hit  : Redis에서 유사도 임계치 이상 결과를 즉시 반환</li>
 *   <li>Miss : PostgreSQL(pgvector) 조회 후 Redis에 저장하고 반환</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CachedVectorSearchStrategy implements SearchStrategy {

    private final BookRepository bookRepository;
    private final EmbeddingService embeddingService;

    @Qualifier("redisVectorStore")
    private final RedisVectorStore redisVectorStore;

    /** 캐시 Hit로 인정할 유사도 임계치 (0.0 ~ 1.0) */
    private static final double SIMILARITY_THRESHOLD = 0.85;

    @Override
    public SearchType supports() {
        return SearchType.VECTOR;
    }

    @Override
    public Page<BookSearchResponse> search(Pageable pageable, BookSearchRequest request) {
        if (!StringUtils.hasText(request.keyword())) {
            return Page.empty(pageable);
        }

        String keyword = request.keyword().trim();
        float[] queryVector = request.vector() != null
                ? request.vector()
                : embeddingService.getEmbedding(keyword);

        log.info("[VectorCache] Redis 캐시 조회 - keyword: '{}'", keyword);

        // 1. Redis 캐시 조회 (유사도 임계치 필터 적용)
        SearchRequest searchRequest = SearchRequest.builder()
                .query(keyword)
                .topK(pageable.getPageSize())
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build();

        List<Document> cachedDocuments = redisVectorStore.similaritySearch(searchRequest);

        // 2. Cache HIT
        if (cachedDocuments != null && !cachedDocuments.isEmpty()) {
            log.info("[VectorCache] Cache HIT - {}건 반환", cachedDocuments.size());
            List<BookSearchResponse> cacheResults = cachedDocuments.stream()
                    .map(this::toSearchResponse)
                    .toList();
            return new PageImpl<>(cacheResults, pageable, cacheResults.size());
        }

        // 3. Cache MISS -> PostgreSQL 조회
        log.info("[VectorCache] Cache MISS - PostgreSQL 조회");
        BookSearchRequest dbRequest = new BookSearchRequest(
                keyword,
                request.isbn(),
                SearchType.VECTOR,
                queryVector,
                request.warmUp()
        );

        Page<BookSearchResponse> dbPage = bookRepository.vectorSearch(pageable, dbRequest);
        List<BookSearchResponse> dbResults = dbPage.getContent();

        // 4. 조회 결과를 Redis 캐시에 저장
        if (!dbResults.isEmpty()) {
            log.info("[VectorCache] {}건을 Redis에 캐싱", dbResults.size());
            List<Document> documentsToCache = dbResults.stream()
                    .map(this::toDocument)
                    .toList();
            redisVectorStore.add(documentsToCache);
        }

        return dbPage;
    }

    /**
     * DTO → Spring AI Document 변환 (Redis 저장용)
     */
    private Document toDocument(BookSearchResponse response) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("bookId", response.id());
        metadata.put("isbn", response.isbn());
        metadata.put("title", response.title());
        metadata.put("volumeTitle", response.volumeTitle());
        metadata.put("authorName", response.authorName());
        metadata.put("publisherName", response.publisherName());
        if (response.price() != null) {
            metadata.put("price", response.price().toString());
        }
        if (response.editionPublishDate() != null) {
            metadata.put("editionPublishDate", response.editionPublishDate().toString());
        }
        metadata.put("imageUrl", response.imageUrl());

        return new Document(response.bookContent(), metadata);
    }

    /**
     * Spring AI Document → DTO 변환 (캐시 복원용)
     */
    private BookSearchResponse toSearchResponse(Document document) {
        Map<String, Object> metadata = document.getMetadata();

        Long id = metadata.get("bookId") != null
                ? ((Number) metadata.get("bookId")).longValue() : null;
        String isbn = (String) metadata.get("isbn");
        String title = (String) metadata.get("title");
        String volumeTitle = (String) metadata.get("volumeTitle");
        String authorName = (String) metadata.get("authorName");
        String publisherName = (String) metadata.get("publisherName");

        BigDecimal price = null;
        if (metadata.get("price") != null) {
            try {
                price = new BigDecimal(metadata.get("price").toString());
            } catch (Exception e) {
                log.warn("[VectorCache] price 파싱 실패", e);
            }
        }

        LocalDate publishDate = null;
        if (metadata.get("editionPublishDate") != null) {
            try {
                publishDate = LocalDate.parse((String) metadata.get("editionPublishDate"));
            } catch (Exception e) {
                log.warn("[VectorCache] editionPublishDate 파싱 실패", e);
            }
        }

        String imageUrl = (String) metadata.get("imageUrl");
        String bookContent = document.getText();
        Double score = (double) document.getScore();

        return new BookSearchResponse(
                id,
                isbn,
                title,
                volumeTitle,
                authorName,
                publisherName,
                price,
                publishDate,
                imageUrl,
                bookContent,
                score
        );
    }
}
