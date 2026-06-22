package com.nhnacademy.springailibrarycore.strategy;

import com.nhnacademy.springailibrarycore.domain.SearchType;
import com.nhnacademy.springailibrarycore.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.repository.BookRepository;
import com.nhnacademy.springailibrarycore.service.embedding.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 질문 임베딩과 도서 임베딩의 의미적 유사도를 이용하는 벡터 검색 전략입니다.
 *
 * 요청에 임베딩이 포함되어 있으면 재사용하고, 없으면 질문에서 새로 생성합니다.
 */
@Component
@RequiredArgsConstructor
public class VectorSearchStrategy implements SearchStrategy {

    private final BookRepository bookRepository;
    private final EmbeddingService embeddingService;

    @Override
    public SearchType supports() {
        return SearchType.VECTOR;
    }

    @Override
    public Page<BookSearchResponse> search(
            Pageable pageable,
            BookSearchRequest request
    ) {
        if (!StringUtils.hasText(request.keyword())) {
            return Page.empty(pageable);
        }

        String keyword = request.keyword().trim();
        float[] vector = request.vector() != null
                ? request.vector()
                : embeddingService.getEmbedding(keyword);

        BookSearchRequest vectorRequest = new BookSearchRequest(
                keyword,
                request.isbn(),
                SearchType.VECTOR,
                vector,
                request.warmUp()
        );

        return bookRepository.vectorSearch(pageable, vectorRequest);
    }
}
