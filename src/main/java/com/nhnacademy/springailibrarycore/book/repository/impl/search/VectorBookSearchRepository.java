package com.nhnacademy.springailibrarycore.book.repository.impl.search;

import com.nhnacademy.springailibrarycore.book.domain.QBook;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.dto.QBookSearchResponse;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/**
 * QueryDSL 벡터 기반 도서 리스트 추출
 */
@Repository
@RequiredArgsConstructor
public class VectorBookSearchRepository {

    private final JPAQueryFactory queryFactory;
    private final QBook book = QBook.book;

    public Page<BookSearchResponse> search(
            Pageable pageable,
            float[] queryVector
    ) {
        if (queryVector == null || queryVector.length == 0) {
            return Page.empty(pageable);
        }
        /**
         * 벡터 -> 문자열 변환
         * 유사도 계산
         * embedding = db에 저장된 임베딩된 도서
         * <=> pgvector의 코사인 거리 연산자
         * CAST 검색어 문자열을 vector로 변환
         * 1-거리: 거리를 유사도로 변경
         * 1.0 -> 매우 유사
         * 0.0 -> 유사 X
         */
        String vector = Arrays.toString(queryVector);
        NumberTemplate<Double> similarity = Expressions.numberTemplate(
                Double.class,
                "function('vector_cosine_similarity', {0})",
                vector
        );

        List<BookSearchResponse> content = queryFactory
                .select(new QBookSearchResponse(
                        book.id,
                        book.isbn,
                        book.title,
                        book.authorName,
                        book.publisherName,
                        book.price,
                        book.imageUrl,
                        book.bookContent,
                        similarity
                ))
                .from(book)
                .where(book.embedding.isNotNull()) // embedding이 없는 도서는 벡터 비교 불가능 함
                .orderBy(similarity.desc()) // 유사도가 높은 순으로 정렬
                .offset(pageable.getOffset()) // 페이지 범위만
                .limit(pageable.getPageSize()) // "
                .fetch();
        /**
         * 전체 도서 수
         * 유사도 몇 이상을 넣을거면
         * where(book.embedding.similarity.goe(0.3)): 0.3 이상만 출력(유사도)
         * goe: [ >= 0.3]
         * gt: [ > 0.3]
         * lt: [ < 0.3]
         * loe: [ <= 0.3]
         * eq: [ = 0.3]
         */
        Long total = queryFactory
                .select(book.count())
                .from(book)
                .where(book.embedding.isNotNull())
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);

    }
}
