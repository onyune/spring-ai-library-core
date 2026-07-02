package com.nhnacademy.springailibrarycore.book.repository;

import com.nhnacademy.springailibrarycore.book.domain.Book;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long>, BookRepositoryCustom {

    Optional<Book> findFirstByTitleContaining(String trimTitle);

    Optional<Book> findByIsbn(String isbn);
}
