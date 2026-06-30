package com.nhnacademy.springailibrarycore.review.repository;

import com.nhnacademy.springailibrarycore.review.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByBookId(Long bookId);

    @Query("SELECT COALESCE(AVG(r.rating), 0.0) FROM Review r WHERE r.book.id = :bookId")
    double getAverageRatingByBookId(@Param("bookId") Long bookId);

    //최근 리뷰 20개만 가져오기
    List<Review> findTop20ByBookIdOrderByCreatedAtDesc(Long bookId);

    //도서의 리뷰 목록 페이징 조회
    Page<Review> findByBookId(Long bookId, Pageable pageable);

    List<Review> findByBookIdAndIdGreaterThanOrderByIdDesc(Long bookId, Long lastReviewId);

    long countByBookIdAndIdGreaterThan(Long bookId, Long lastReviewId);

    boolean existsByBookIdAndReviewerId(Long bookId, String reviewerId);

    long countByBookId(Long bookId);
}

