package com.nhnacademy.springailibrarycore.book.repository.impl.search;

import com.nhnacademy.springailibrarycore.book.domain.QBook;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class KeywordBookSearchRepository {

    private final JPAQueryFactory queryFactory;
    private final QBook book = QBook.book;

    public Page<BookSearchResponse> search(
            Pageable pageable,
            BookSearchRequest request
    ) {
        BooleanBuilder condition = createSearchCondition(request);

        JPAQuery<BookSearchResponse> contentQuery = queryFactory
                .select(Projections.constructor(
                        BookSearchResponse.class,
                        book.id,
                        book.isbn,
                        book.title,
                        book.volumeTitle,
                        book.authorName,
                        book.publisherName,
                        book.price,
                        book.editionPublishDate,
                        book.imageUrl,
                        book.bookContent
                ))
                .from(book)
                .where(condition);

        if (StringUtils.hasText(request.keyword())) {
            NumberExpression<Integer> relevanceScore =
                    createRelevanceScore(request.keyword().trim());
            contentQuery.orderBy(relevanceScore.desc(), book.id.asc());
        } else {
            contentQuery.orderBy(book.id.asc());
        }

        List<BookSearchResponse> content = contentQuery
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(book.count())
                .from(book)
                .where(condition)
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    // 검색어에 대한 관련도 순서 결정 -> 점수
    private NumberExpression<Integer> createRelevanceScore(String keyword) {
        // 제목 점수
        NumberExpression<Integer> titleScore = new CaseBuilder()
                .when(book.title.equalsIgnoreCase(keyword)).then(100)
                .when(book.title.startsWithIgnoreCase(keyword)).then(80)
                .when(book.title.containsIgnoreCase(keyword)).then(60)
                .otherwise(0);
        // 권차 제목, 부제목 점수
        NumberExpression<Integer> volumeAndSubtitleScore = new CaseBuilder()
                .when(book.volumeTitle.containsIgnoreCase(keyword)).then(40)
                .when(book.subtitle.containsIgnoreCase(keyword)).then(40)
                .otherwise(0);
        // 작가 점수
        NumberExpression<Integer> authorScore = new CaseBuilder()
                .when(book.authorName.containsIgnoreCase(keyword)).then(30)
                .otherwise(0);
        // 출판사 점수
        NumberExpression<Integer> publisherScore = new CaseBuilder()
                .when(book.publisherName.containsIgnoreCase(keyword)).then(20)
                .otherwise(0);
        // 책 설명 점수
        NumberExpression<Integer> contentScore = new CaseBuilder()
                .when(book.bookContent.containsIgnoreCase(keyword)).then(10)
                .otherwise(0);

        return titleScore
                .add(volumeAndSubtitleScore)
                .add(authorScore)
                .add(publisherScore)
                .add(contentScore);
    }

    // 검색 포함 여부
    private BooleanBuilder createSearchCondition(BookSearchRequest request) {
        BooleanBuilder condition = new BooleanBuilder();

        if (StringUtils.hasText(request.keyword())) {
            String keyword = request.keyword().trim();
            /**
             * 검색 결과 정렬
             * 이미 포함된 도서 중 어떤 도서를 먼저 보여줄지
             */
            condition.and(
                    book.title.containsIgnoreCase(keyword)
                            .or(book.volumeTitle.containsIgnoreCase(keyword))
                            .or(book.subtitle.containsIgnoreCase(keyword))
                            .or(book.authorName.containsIgnoreCase(keyword))
                            .or(book.publisherName.containsIgnoreCase(keyword))
                            .or(book.bookContent.containsIgnoreCase(keyword))
            );
        }

        if (StringUtils.hasText(request.isbn())) {
            condition.and(book.isbn.eq(request.isbn().trim()));
        }

        return condition;
    }
}
