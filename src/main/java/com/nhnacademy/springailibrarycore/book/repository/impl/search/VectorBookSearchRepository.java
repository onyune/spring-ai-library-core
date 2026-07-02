package com.nhnacademy.springailibrarycore.book.repository.impl.search;

import com.nhnacademy.springailibrarycore.book.domain.QBook;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchPageResult;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.dto.QBookSearchResponse;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberTemplate;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${vector.similarity.threshold}")
    private double similarityThreshold;

    public BookSearchPageResult search(
            Pageable pageable,
            float[] queryVector
    ) {
        if (queryVector == null || queryVector.length == 0) {
            return new BookSearchPageResult(List.of(), 0);
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
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < queryVector.length; i++) {
            sb.append(String.format("%.6f", queryVector[i]));
            if (i < queryVector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        String vector = sb.toString();
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
                .where(
                        book.embedding.isNotNull(),
                        similarity.goe(similarityThreshold) // 유사도가 THRESHOLD 이상인 것만 필터링 (Greater Or Equal)
                ) // embedding이 없는 도서는 벡터 비교 불가능 함
                .orderBy(similarity.desc()) // 유사도가 높은 순으로 정렬
                .offset(pageable.getOffset()) // 페이지 범위만
                .limit(pageable.getPageSize()) // "
                .fetch();

        Long total = queryFactory
                .select(book.count())
                .from(book)
                .where(
                        book.embedding.isNotNull(),
                        similarity.goe(similarityThreshold) // 동일한 조건 추가
                )
                .fetchOne();

        return new BookSearchPageResult(content, total == null ? 0 : total);
    }

    public List<BookSearchResponse> findPersonalizedBooks(float[] queryVector, int limit) {
        if (queryVector == null || queryVector.length == 0) {
            return Collections.emptyList();
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < queryVector.length; i++) {
            sb.append(String.format("%.6f", queryVector[i]));
            if (i < queryVector.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        String vector = sb.toString();
        NumberTemplate<Double> similarity = Expressions.numberTemplate(
                Double.class,
                "function('vector_cosine_similarity', {0})",
                vector
        );

        return queryFactory
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
                .where(book.embedding.isNotNull()) // Threshold 검사 생략 (평균 벡터라 코사인 유사도가 상대적으로 낮음)
                .orderBy(similarity.desc())
                .limit(limit)
                .fetch();
    }

    public List<float[]> findEmbeddingsByBookIds(List<Long> bookIds){
        if(bookIds==null || bookIds.isEmpty()){
            return Collections.emptyList();
        }
        return queryFactory
                .select(book.embedding)
                .from(book)
                .where(book.id.in(bookIds),
                        book.embedding.isNotNull())
                .fetch();
    }

    public java.util.Map<Long, float[]> findEmbeddingMapByBookIds(List<Long> bookIds){
        if(bookIds==null || bookIds.isEmpty()){
            return Collections.emptyMap();
        }
        List<com.querydsl.core.Tuple> results = queryFactory
                .select(book.id, book.embedding)
                .from(book)
                .where(book.id.in(bookIds),
                        book.embedding.isNotNull())
                .fetch();

        java.util.Map<Long, float[]> map = new java.util.HashMap<>();
        for (com.querydsl.core.Tuple tuple : results) {
            map.put(tuple.get(book.id), tuple.get(book.embedding));
        }
        return map;
    }
}
