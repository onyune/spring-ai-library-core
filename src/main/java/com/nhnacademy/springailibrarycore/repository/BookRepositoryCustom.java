package com.nhnacademy.springailibrarycore.repository;


import com.nhnacademy.springailibrarycore.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.dto.BookSearchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BookRepositoryCustom {

    Page<BookSearchResponse> search(Pageable pageable, BookSearchRequest request);

    Page<BookSearchResponse> vectorSearch(Pageable pageable, BookSearchRequest request);
}
