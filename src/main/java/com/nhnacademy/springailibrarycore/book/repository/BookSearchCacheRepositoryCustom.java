package com.nhnacademy.springailibrarycore.book.repository;

import com.nhnacademy.springailibrarycore.book.domain.BookSearchCache;
import com.nhnacademy.springailibrarycore.book.domain.SearchType;
import java.util.Optional;

/**
 * Spring Data JPA 기본 기능으로 표현하기 어려운 시맨틱 캐시 조회 계약입니다.
 *
 * pgvector 코사인 거리 기반의 최적 캐시 검색과 TTL이 만료된 캐시 삭제 기능을
 * 정의합니다.
 */
public interface BookSearchCacheRepositoryCustom {

    Optional<BookSearchCache> findBestMatch(
            SearchType searchType,
            float[] queryEmbedding,
            double similarityThreshold
    );

    int deleteExpired();
}
