package com.nhnacademy.springailibrarycore.strategy;

import com.nhnacademy.springailibrarycore.domain.SearchType;
import com.nhnacademy.springailibrarycore.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.repository.BookRepository;
import com.nhnacademy.springailibrarycore.service.embedding.EmbeddingService;
import com.nhnacademy.springailibrarycore.service.search.RrfFusionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 키워드 검색과 벡터 검색 결과를 RRF 점수로 결합하는 하이브리드 검색 전략입니다.
 * 각 검색 결과의 순위를 점수로 환산해 합산하고, 종합 점수가 높은 도서부터
 * 반환합니다.
 */
@Component
@RequiredArgsConstructor
public class HybridSearchStrategy implements SearchStrategy {

    /**
     * CANDIDATE_SIZE = 후보 도서 수
     */
    private static final int CANDIDATE_SIZE = 100;

    private final BookRepository bookRepository;
    private final EmbeddingService embeddingService;
    private final RrfFusionService rrfFusionService;

    @Override
    public SearchType supports() {
        return SearchType.HYBRID;
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
        Pageable candidatePage = PageRequest.of(0, CANDIDATE_SIZE);

        BookSearchRequest keywordRequest = new BookSearchRequest(
                keyword,
                request.isbn(),
                SearchType.KEYWORD,
                null,
                request.warmUp()
        );

        List<BookSearchResponse> keywordResults =
                bookRepository.search(candidatePage, keywordRequest).getContent();

        float[] queryVector = request.vector() != null
                ? request.vector()
                : embeddingService.getEmbedding(keyword);
        BookSearchRequest vectorRequest = new BookSearchRequest(
                keyword,
                request.isbn(),
                SearchType.VECTOR,
                queryVector,
                request.warmUp()
        );
        List<BookSearchResponse> vectorResults =
                bookRepository.vectorSearch(candidatePage, vectorRequest).getContent();

        List<BookSearchResponse> rankedResults =
                rrfFusionService.fuse(keywordResults, vectorResults);

        return toPage(rankedResults, pageable);
    }

    private Page<BookSearchResponse> toPage(
            List<BookSearchResponse> results,
            Pageable pageable
    ) {
        int start = Math.toIntExact(pageable.getOffset());
        if (start >= results.size()) {
            return new PageImpl<>(List.of(), pageable, results.size());
        }

        int end = Math.min(start + pageable.getPageSize(), results.size());
        return new PageImpl<>(
                results.subList(start, end),
                pageable,
                results.size()
        );
    }
}
