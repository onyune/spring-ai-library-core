package com.nhnacademy.springailibrarycore.book.repository.impl;

import com.nhnacademy.springailibrarycore.book.domain.BookSearchCache;
import com.nhnacademy.springailibrarycore.book.repository.BookSearchCacheRepositoryCustom;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

/**
 * PostgreSQL과 pgvector를 이용하여 시맨틱 캐시 전용 쿼리를 실행하는 저장소 구현체입니다.
 *
 * 미만료 캐시 중 질문 임베딩과 가장 유사한 항목을 조회하고, TTL이 지난 항목을
 * 데이터베이스에서 삭제합니다.
 */
public class BookSearchCacheRepositoryImpl
        implements BookSearchCacheRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 최적 검색 쿼리
     * @param queryEmbedding 임베딩된 질문
     * @param similarityThreshold 유사도 임계치
     * @return 캐싱 객체
     */
    @Override
    public Optional<BookSearchCache> findBestMatch(
            float[] queryEmbedding,
            double similarityThreshold
    ) {
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            return Optional.empty();
        }

        List<BookSearchCache> matches = entityManager.createNativeQuery("""
                        SELECT cache.*
                        FROM book_search_cache cache
                        WHERE cache.expires_at > CURRENT_TIMESTAMP 
                          AND 1.0 - (
                              cache.query_embedding
                              <=> CAST(:queryEmbedding AS vector)
                          ) >= :similarityThreshold
                        ORDER BY cache.query_embedding
                                 <=> CAST(:queryEmbedding AS vector)
                        LIMIT 1
                        """, BookSearchCache.class)
                .setParameter(
                        "queryEmbedding",
                        Arrays.toString(queryEmbedding)
                )
                .setParameter(
                        "similarityThreshold",
                        similarityThreshold
                )
                .getResultList();

        return matches.stream().findFirst();
    }

    @Override
    @Transactional
    public int deleteExpired() {
        return entityManager.createNativeQuery("""
                        DELETE FROM book_search_cache
                        WHERE expires_at <= CURRENT_TIMESTAMP
                        """)
                .executeUpdate();
    }
}
