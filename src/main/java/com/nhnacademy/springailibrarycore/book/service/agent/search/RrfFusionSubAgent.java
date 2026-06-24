package com.nhnacademy.springailibrarycore.book.service.agent.search;

import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Reciprocal Rank Fusion(RRF) 알고리즘으로 키워드 결과와 벡터 결과를 결합합니다.
 *
 * RRF 점수 공식: score(d) = Σ 1 / (k + rank(d))
 * k = 60 (기본값, 상위 랭킹에 지나치게 높은 가중치가 쏠리는 것을 방지)
 */
@Service
public class RrfFusionSubAgent {

    private static final int K = 60;

    /**
     * 키워드 검색 결과와 벡터 검색 결과를 RRF 점수로 결합합니다.
     *
     * @param keywordResults 키워드 검색 결과 (순위 순)
     * @param vectorResults  벡터 검색 결과 (유사도 순)
     * @return RRF 점수가 높은 순으로 정렬된 통합 결과 List<BookSearchResponse>
     */
    public List<BookSearchResponse> fuse(
            List<BookSearchResponse> keywordResults,
            List<BookSearchResponse> vectorResults
    ) {
        Map<Long, Double> rrfScoreMap = new HashMap<>();

        // 키워드 검색 결과에 RRF 점수 부여
        for (int rank = 0; rank < keywordResults.size(); rank++) {
            BookSearchResponse book = keywordResults.get(rank);
            if (book.getId() != null) {
                rrfScoreMap.merge(book.getId(), 1.0 / (K + rank + 1), Double::sum);
            }
        }

        // 벡터 검색 결과에 RRF 점수 추가 합산
        for (int rank = 0; rank < vectorResults.size(); rank++) {
            BookSearchResponse book = vectorResults.get(rank);
            if (book.getId() != null) {
                rrfScoreMap.merge(book.getId(), 1.0 / (K + rank + 1), Double::sum);
            }
        }

        // 두 리스트를 합쳐 중복 제거 후 RRF 점수 부착
        Map<Long, BookSearchResponse> bookMap = new HashMap<>();
        for (BookSearchResponse book : keywordResults) {
            if (book.getId() != null) {
                bookMap.put(book.getId(), book);
            }
        }
        for (BookSearchResponse book : vectorResults) {
            if (book.getId() != null) {
                bookMap.putIfAbsent(book.getId(), book);
            }
        }

        // RRF 점수를 DTO에 반영하여 내림차순 정렬
        List<BookSearchResponse> result = new ArrayList<>();
        for (Map.Entry<Long, BookSearchResponse> entry : bookMap.entrySet()) {
            Double score = rrfScoreMap.getOrDefault(entry.getKey(), 0.0);
            result.add(entry.getValue().withRrfScore(score));
        }

        result.sort(Comparator.comparingDouble(
                (BookSearchResponse b) -> b.getRrfScore() != null ? b.getRrfScore() : 0.0
        ).reversed());

        return result;
    }
}
