package com.nhnacademy.springailibrarycore.repository;

import com.nhnacademy.library.core.book.domain.Book;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long>, BookRepositoryCustom {

    List<Book> findAllByTitleIsNotNullAndBookContentIsNotNullAndEmbeddingIsNull(
            Pageable pageable
    );

    @Query("SELECT b.embedding FROM Book b WHERE b.id IN :bookIds AND b.embedding IS NOT NULL")
    List<float[]> findEmbeddingsByIds(@Param("bookIds") List<Long> bookIds);
}
