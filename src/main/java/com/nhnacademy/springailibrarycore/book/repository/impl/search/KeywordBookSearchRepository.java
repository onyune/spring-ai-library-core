package com.nhnacademy.springailibrarycore.book.repository.impl.search;

import com.nhnacademy.springailibrarycore.book.domain.QBook;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchPageResult;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.dto.QBookSearchResponse;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * QueryDSL 동적 쿼리 기반 키워드 도서 검색 리포지토리입니다.
 *
 * [PostgreSQL 전문 검색(Full Text Search, FTS) 동작 방식 및 주의사항]
 * 본 클래스는 긴 텍스트(예: 책 본문) 검색 시 발생하는 풀 테이블 스캔(Full Table Scan)을 방지하기 위해 
 * PostgreSQL의 FTS 기능(ts_match_korean)과 GIN 인덱스를 활용합니다.
 *
 * - 내부 동작 원리:
 *   plainto_tsquery 함수는 입력된 검색어를 형태소 단위로 쪼갠 뒤 모두 AND(&) 조건으로 묶습니다.
 *   예를 들어 "자바 스프링 검색해줘"가 입력되면 '자바' & '스프링' & '검색' & '해주' 로 변환됩니다.
 *
 * - 어휘 불일치(Vocabulary Mismatch) 문제:
 *   사용자의 자연어 질의("검색해줘", "찾아줘" 등)가 그대로 들어가면, 본문 내에 해당 불용어가 없는 일반적인 기술 서적은 
 *   결과가 0건으로 매칭 실패하게 됩니다.
 *
 * - 해결 방안(전처리 필수):
 *   따라서 이 리포지토리에 검색어(keyword)를 넘기기 전에, 반드시 형태소 분석기나 LLM을 통해 
 *   불용어를 제거하고 핵심 명사(예: "자바 스프링")만 추출하는 전처리(Query Processing) 과정이 수반되어야 
 *   FTS 엔진이 정상적으로 동작하여 정확도 높은 결과를 반환할 수 있습니다.
 */
@Repository
@RequiredArgsConstructor
public class KeywordBookSearchRepository {

    private final JPAQueryFactory queryFactory;
    private final QBook book = QBook.book;

    public BookSearchPageResult search(
            Pageable pageable,
            BookSearchRequest request
    ) {
        BooleanBuilder condition = createSearchCondition(request);

        JPAQuery<BookSearchResponse> contentQuery = queryFactory
                .select(new QBookSearchResponse(
                        book.id,
                        book.isbn,
                        book.title,
                        book.authorName,
                        book.publisherName,
                        book.price,
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

        return new BookSearchPageResult(content, total == null ? 0 : total);
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
        // 책 설명 전문 검색(FTS) 점수
        BooleanExpression ftsMatch = Expressions.booleanTemplate(
                "function('ts_match_korean', {0}, {1}) = true",
                book.bookContent, keyword
        );
        NumberExpression<Integer> contentScore = new CaseBuilder()
                .when(ftsMatch).then(10)
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
                            .or(Expressions.booleanTemplate("function('ts_match_korean', {0}, {1}) = true", book.bookContent, keyword))
            );
            // WHERE title LIKE %keyword% 인덱스 안탐
            // WHERE title LIKE %keyword 아래 두개만 인덱스가 됨
            // WHERE title LIKE keyword%

        }

        if (StringUtils.hasText(request.isbn())) {
            condition.and(book.isbn.eq(request.isbn().trim()));
        }

        return condition;
    }
}
