package com.nhnacademy.springailibrarycore.book.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * RAG 도서 추천 결과를 PostgreSQL에 저장하는 시맨틱 캐시 엔티티입니다.
 *
 * 사용자 질문의 임베딩을 pgvector 컬럼에 저장하고, 추천 결과는 JSON 문자열로
 * 보관합니다.
 */
@Entity
@Table(name = "book_search_cache")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookSearchCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "query", nullable = false, length = 500)
    private String query;

    @Convert(converter = VectorConverter.class)
    @Column(
            name = "query_embedding",
            nullable = false,
            columnDefinition = "vector(1024)"
    )
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private float[] queryEmbedding;

    @Column(name = "result", nullable = false, columnDefinition = "TEXT")
    private String result;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt; // 만료시간까지 계산된 컬럼 createAt + ttlSeconds

    @Column(name = "last_accessed_at", nullable = false)
    private OffsetDateTime lastAccessedAt;

    @Column(name = "access_count", nullable = false)
    private int accessCount;

    @Column(name = "ttl_seconds", nullable = false)
    private int ttlSeconds;

    private BookSearchCache(
            String query,
            float[] queryEmbedding,
            String result,
            OffsetDateTime createdAt,
            int ttlSeconds
    ) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("캐시 질문은 비어 있을 수 없습니다.");
        }
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            throw new IllegalArgumentException("질문 임베딩은 비어 있을 수 없습니다.");
        }
        if (result == null || result.isBlank()) {
            throw new IllegalArgumentException("캐시 결과는 비어 있을 수 없습니다.");
        }
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("TTL은 0보다 커야 합니다.");
        }

        this.query = query.trim();
        this.queryEmbedding = queryEmbedding.clone();
        this.result = result;
        this.createdAt = createdAt;
        this.expiresAt = createdAt.plusSeconds(ttlSeconds);
        this.lastAccessedAt = createdAt;
        this.ttlSeconds = ttlSeconds;
    }

    public static BookSearchCache create(
            String query,
            float[] queryEmbedding,
            String result,
            OffsetDateTime createdAt,
            int ttlSeconds
    ) {
        return new BookSearchCache(
                query,
                queryEmbedding,
                result,
                createdAt,
                ttlSeconds
        );
    }

    public void recordAccess(OffsetDateTime accessedAt) {
        this.lastAccessedAt = accessedAt;
        this.accessCount++;
    }

    public float[] getQueryEmbedding() {
        return queryEmbedding.clone();
    }

    @PrePersist // 캐시 객체가 생성되는 시점에
    void initializeTimestamps() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (lastAccessedAt == null) {
            lastAccessedAt = createdAt;
        }
        if (expiresAt == null) {
            expiresAt = createdAt.plusSeconds(ttlSeconds);
        }
    }
}
