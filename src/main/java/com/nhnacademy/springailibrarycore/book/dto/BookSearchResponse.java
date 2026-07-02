package com.nhnacademy.springailibrarycore.book.dto;

import com.nhnacademy.springailibrarycore.book.domain.Book;
import com.querydsl.core.annotations.QueryProjection;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * 키워드, 벡터, 하이브리드 검색에서 공통으로 사용하는 응답 DTO.
 *
 * <p>{@code @Builder(toBuilder = true)}를 사용하여 불변 복사(wither 패턴)를 안전하게 수행합니다.
 * 명시적으로 세팅하지 않은 필드는 {@code null}로 초기화되므로 하드코딩 누락이 발생하지 않습니다.
 */
@Getter
@Jacksonized
@Builder(toBuilder = true)
public class BookSearchResponse {

    private final Long id;
    private final String isbn;
    private final String title;
    private final String authorName;
    private final String publisherName;
    private final BigDecimal price;
    private final String imageUrl;

    /** 도서 소개 내용. AI 추천 사유 생성 컨텍스트로 활용됩니다. */
    private final String bookContent;

    /** 벡터 검색 유사도 (0.0 ~ 1.0). 키워드 검색 결과에서는 null. */
    private final Double similarity;

    /** RRF 결합 점수. 하이브리드 검색 결과에서만 사용. */
    private final Double rrfScore;

    /** AI 연관성 점수 (0~100). RAG 검색 결과에서만 사용. */
    private final Integer relevance;

    /** AI 추천 사유. RAG 검색 결과에서만 사용. */
    private final String aiComment;

    /** 개인화 추천 유사도 점수. 이 값이 null이 아니면 개인화 추천 UI를 노출합니다. */
    private final Double personalizationScore;

    /**
     * 키워드 검색용 QueryDSL 프로젝션 생성자.
     * similarity, rrfScore, relevance, aiComment 는 null로 초기화.
     */
    @QueryProjection
    public BookSearchResponse(
            Long id,
            String isbn,
            String title,
            String authorName,
            String publisherName,
            BigDecimal price,
            String imageUrl,
            String bookContent
    ) {
        this(id, isbn, title, authorName, publisherName,
                price, imageUrl, bookContent,
                null, null, null, null, null);
    }

    /**
     * 벡터 검색용 QueryDSL 프로젝션 생성자.
     * rrfScore, relevance, aiComment 는 null로 초기화됩니다.
     */
    @QueryProjection
    public BookSearchResponse(
            Long id,
            String isbn,
            String title,
            String authorName,
            String publisherName,
            BigDecimal price,
            String imageUrl,
            String bookContent,
            Double similarity
    ) {
        this(id, isbn, title, authorName, publisherName,
                price, imageUrl, bookContent,
                similarity, null, null, null, null);
    }

    /**
     * 전체 필드 생성자. @Builder가 이 생성자를 사용합니다.
     */
    public BookSearchResponse(
            Long id,
            String isbn,
            String title,
            String authorName,
            String publisherName,
            BigDecimal price,
            String imageUrl,
            String bookContent,
            Double similarity,
            Double rrfScore,
            Integer relevance,
            String aiComment,
            Double personalizationScore
    ) {
        this.id = id;
        this.isbn = isbn;
        this.title = title;
        this.authorName = authorName;
        this.publisherName = publisherName;
        this.price = price;
        this.imageUrl = imageUrl;
        this.bookContent = bookContent;
        this.similarity = similarity;
        this.rrfScore = rrfScore;
        this.relevance = relevance;
        this.aiComment = aiComment;
        this.personalizationScore = personalizationScore;
    }

    /**
     * Book 엔티티로부터 검색 응답 DTO를 생성합니다.
     * 검색 점수 필드(similarity, rrfScore, relevance, aiComment)는 null로 초기화됩니다.
     */
    public static BookSearchResponse from(Book book) {
        return BookSearchResponse.builder()
                .id(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .authorName(book.getAuthorName())
                .publisherName(book.getPublisherName())
                .price(book.getPrice())
                .imageUrl(book.getImageUrl())
                .bookContent(book.getBookContent())
                .build();
    }

    /**
     * RRF 점수를 교체한 새 인스턴스를 반환합니다.
     * toBuilder()를 통해 나머지 필드는 그대로 복사됩니다.
     */
    public BookSearchResponse withRrfScore(Double rrfScore) {
        return this.toBuilder()
                .rrfScore(rrfScore)
                .build();
    }

    /**
     * 유사도 점수를 교체한 새 인스턴스를 반환합니다.
     * toBuilder()를 통해 나머지 필드는 그대로 복사됩니다.
     */
    public BookSearchResponse withSimilarity(Double similarity) {
        return this.toBuilder()
                .similarity(similarity)
                .build();
    }

    /**
     * AI 추천 정보를 교체한 새 인스턴스를 반환합니다.
     * toBuilder()를 통해 나머지 필드는 그대로 복사됩니다.
     */
    public BookSearchResponse withAiComment(Integer relevance, String aiComment) {
        return this.toBuilder()
                .relevance(relevance)
                .aiComment(aiComment)
                .build();
    }

    /**
     * 개인화 추천 점수를 교체한 새 인스턴스를 반환합니다.
     * toBuilder()를 통해 나머지 필드는 그대로 복사됩니다.
     */
    public BookSearchResponse withPersonalizationScore(Double personalizationScore) {
        return this.toBuilder()
                .personalizationScore(personalizationScore)
                .build();
    }
}
