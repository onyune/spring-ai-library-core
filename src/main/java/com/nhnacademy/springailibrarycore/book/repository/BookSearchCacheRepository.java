package com.nhnacademy.springailibrarycore.book.repository;

import com.nhnacademy.springailibrarycore.book.domain.BookSearchCache;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 도서 추천 시맨틱 캐시의 기본 CRUD와 pgvector 전용 조회 기능을 제공하는 저장소입니다.
 */
public interface BookSearchCacheRepository
        extends JpaRepository<BookSearchCache, Long>,
        BookSearchCacheRepositoryCustom {
}
