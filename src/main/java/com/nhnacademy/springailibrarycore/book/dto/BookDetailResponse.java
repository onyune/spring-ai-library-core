package com.nhnacademy.springailibrarycore.book.dto;

import com.nhnacademy.springailibrarycore.book.domain.Book;
import com.nhnacademy.springailibrarycore.review.dto.ReviewResponse;
import org.springframework.data.domain.Page;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BookDetailResponse(
        Long id,
        String isbn,
        String title,
        String volumeTitle,
        String authorName,
        String publisherName,
        BigDecimal price,
        String imageUrl,
        String bookContent,
        LocalDate firstPublishDate,
        Page<ReviewResponse> reviews
) {
    public static BookDetailResponse from(Book book, Page<ReviewResponse> reviews) {
        return new BookDetailResponse(
                book.getId(),
                book.getIsbn(),
                book.getTitle(),
                book.getVolumeTitle(),
                book.getAuthorName(),
                book.getPublisherName(),
                book.getPrice(),
                book.getImageUrl(),
                book.getBookContent(),
                book.getFirstPublishDate(),
                reviews
        );
    }
}

