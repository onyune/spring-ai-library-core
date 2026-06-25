package com.nhnacademy.springailibrarycore.book.repository;

import com.nhnacademy.springailibrarycore.book.domain.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long>, BookRepositoryCustom {

}
