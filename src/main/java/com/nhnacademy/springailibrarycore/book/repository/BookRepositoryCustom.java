package com.nhnacademy.springailibrarycore.book.repository;


import com.nhnacademy.springailibrarycore.book.dto.BookSearchPageResult;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BookRepositoryCustom {

    BookSearchPageResult search(Pageable pageable, BookSearchRequest request);

    BookSearchPageResult vectorSearch(Pageable pageable, BookSearchRequest request);
}
