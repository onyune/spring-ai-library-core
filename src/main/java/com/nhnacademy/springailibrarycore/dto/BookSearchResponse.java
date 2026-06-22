package com.nhnacademy.springailibrarycore.dto;

import com.nhnacademy.springailibrarycore.domain.Book;
import com.querydsl.core.annotations.QueryProjection;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 키워드, 벡터, 하이브리드 검색에서 공통으로 사용하는 응답 DTO.
 */
public record BookSearchResponse(
        Long id,
        String isbn,
        String title,
        String volumeTitle,
        String authorName,
        String publisherName,
        BigDecimal price,
        LocalDate editionPublishDate,
        String imageUrl,
        String bookContent,
        Double similarity,
        Double rrfScore
) {

    @QueryProjection
    public BookSearchResponse(
            Long id,
            String isbn,
            String title,
            String volumeTitle,
            String authorName,
            String publisherName,
            BigDecimal price,
            LocalDate editionPublishDate,
            String imageUrl,
            String bookContent
    ) {
        this(
                id,
                isbn,
                title,
                volumeTitle,
                authorName,
                publisherName,
                price,
                editionPublishDate,
                imageUrl,
                bookContent,
                null,
                null
        );
    }

    @QueryProjection
    public BookSearchResponse(
            Long id,
            String isbn,
            String title,
            String volumeTitle,
            String authorName,
            String publisherName,
            BigDecimal price,
            LocalDate editionPublishDate,
            String imageUrl,
            String bookContent,
            Double similarity
    ) {
        this(
                id,
                isbn,
                title,
                volumeTitle,
                authorName,
                publisherName,
                price,
                editionPublishDate,
                imageUrl,
                bookContent,
                similarity,
                null
        );
    }

    public static BookSearchResponse from(Book book) {
        return new BookSearchResponse(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getVolumeTitle(),
                book.getAuthorName(),
                book.getPublisherName(),
                book.getPrice(),
                book.getEditionPublishDate(),
                book.getImageUrl(),
                book.getBookContent(),
                null,
                null
        );
    }

    // 불변 수정을 위한 Wither 메서드
    public BookSearchResponse withRrfScore(Double rrfScore) {
        return new BookSearchResponse(
                this.id,
                this.isbn,
                this.title,
                this.volumeTitle,
                this.authorName,
                this.publisherName,
                this.price,
                this.editionPublishDate,
                this.imageUrl,
                this.bookContent,
                this.similarity,
                rrfScore
        );
    }

    public BookSearchResponse withSimilarity(Double similarity) {
        return new BookSearchResponse(
                this.id,
                this.isbn,
                this.title,
                this.volumeTitle,
                this.authorName,
                this.publisherName,
                this.price,
                this.editionPublishDate,
                this.imageUrl,
                this.bookContent,
                similarity,
                this.rrfScore
        );
    }
}
