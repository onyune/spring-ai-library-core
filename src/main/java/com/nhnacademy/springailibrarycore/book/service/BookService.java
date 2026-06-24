package com.nhnacademy.springailibrarycore.book.service;

import com.nhnacademy.springailibrarycore.book.domain.Book;
import com.nhnacademy.springailibrarycore.book.dto.BookDetailResponse;
import com.nhnacademy.springailibrarycore.book.repository.BookRepository;
import com.nhnacademy.springailibrarycore.review.dto.ReviewResponse;
import com.nhnacademy.springailibrarycore.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {

    private final BookRepository bookRepository;
    private final ReviewService reviewService;

    /**
     * 특정 도서의 상세 정보와 리뷰 목록을 함께 조회합니다.
     *
     * @param bookId   도서 ID
     * @param pageable 리뷰 페이징 및 정렬 정보
     * @return BookDetailResponse 도서 상세 정보 DTO
     */
    @Transactional(readOnly = true)
    public BookDetailResponse getBookDetail(Long bookId, Pageable pageable) {
        log.info("[BookService] 도서 상세 및 리뷰 페이징 조회 요청 -> id: {}, page: {}", bookId, pageable.getPageNumber());

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 도서 ID입니다. (ID: " + bookId + ")"));

        // 리뷰 목록 페이징 조회
        Page<ReviewResponse> reviews = reviewService.getReviewsByBookId(bookId, pageable);

        return BookDetailResponse.from(book, reviews);
    }
}
