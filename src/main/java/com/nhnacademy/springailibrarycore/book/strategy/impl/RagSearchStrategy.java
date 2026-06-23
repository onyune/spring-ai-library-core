package com.nhnacademy.springailibrarycore.book.strategy.impl;

import com.nhnacademy.springailibrarycore.book.domain.SearchType;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.service.agent.embedding.EmbeddingSubAgent;
import com.nhnacademy.springailibrarycore.book.strategy.SearchStrategy;
import com.nhnacademy.springailibrarycore.book.strategy.support.RrfBookReranker;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * RAG 프롬프트에 전달할 도서 후보를 선별하는 검색 전략입니다.
 *
 * 하이브리드 검색으로 Retrieval 후보를 확보하고, RRF 임계값 필터링과 정렬을 거쳐
 * 상위 Rerank K개의 도서만 반환합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RagSearchStrategy implements SearchStrategy {
    private final RrfBookReranker rrfBookReranker;
    private final EmbeddingSubAgent embeddingSubAgent;

    @Override
    public SearchType supports() {
        return SearchType.RAG;
    }

    @Override
    public Page<BookSearchResponse> search(
            Pageable pageable,
            BookSearchRequest request
    ) {
        String normalizedQuestion = request.keyword();
        float[] questionVector = embeddingSubAgent.getEmbedding(normalizedQuestion);


        List<BookSearchResponse> responses = rrfBookReranker.reranker(request);


    }
}
