package com.nhnacademy.springailibrarycore.book.repository;


import com.nhnacademy.springailibrarycore.book.dto.BookSearchPageResult;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BookRepositoryCustom {

    BookSearchPageResult search(Pageable pageable, BookSearchRequest request);

    BookSearchPageResult vectorSearch(Pageable pageable, BookSearchRequest request);

    List<float[]> findEmbeddingByBookIds(List<Long> bookIds);

    java.util.Map<Long, float[]> findEmbeddingMapByBookIds(List<Long> bookIds);
}
