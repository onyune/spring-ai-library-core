package com.nhnacademy.springailibrarycore.book.service.agent.search;

import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Reciprocal Rank Fusion(RRF) 알고리즘으로 키워드 결과와 벡터 결과를 결합합니다.
 *
 * [왜 RRF가 필요한가?]
 * 키워드 검색 점수(단어 빈도 기반)와 벡터 검색 점수(코사인 유사도 기반)는 척도와 단위가 완전히 다릅니다.
 * 단순 점수 합산으로는 공정한 결과를 낼 수 없으므로, 원본 점수를 무시하고 오직 '순위(Rank)'만을 기반으로 재점수를 매깁니다.
 *
 * [RRF 점수 계산 공식]
 * Score = Σ 1 / (K + 해당 검색의 등수)
 *
 * - K (상수): 60 (상위권에 점수가 극단적으로 쏠리는 것을 방지하기 위한 학계 권장값)
 * 
 * - 장점: 한쪽 검색에서만 높은 순위를 차지한 결과보다, 양쪽 모두에서 준수한 순위를 기록한 결과가 
 * 더 높은 최종 점수를 얻게 되어 문맥적, 키워드적 의도가 모두 일치하는 최상의 결과를 도출합니다.
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
        // 해시맵 크기를 미리 할당하여 리사이징(Re-hashing) 오버헤드 방지
        int expectedSize = keywordResults.size() + vectorResults.size();
        int initialCapacity = (int) (expectedSize / 0.75f) + 1;
        
        Map<Long, Double> rrfScoreMap = new HashMap<>(initialCapacity);
        Map<Long, BookSearchResponse> bookMap = new HashMap<>(initialCapacity);

        // 1. 키워드 검색 결과 RRF 점수 부여 및 원본 저장 (루프 통합)
        int keywordSize = keywordResults.size();
        for (int rank = 0; rank < keywordSize; rank++) {
            BookSearchResponse book = keywordResults.get(rank);
            Long id = book.getId();
            if (id != null) {
                rrfScoreMap.put(id, 1.0 / (K + rank + 1));
                bookMap.put(id, book);
            }
        }

        // 2. 벡터 검색 결과 RRF 점수 합산 및 원본 저장 (루프 통합)
        int vectorSize = vectorResults.size();
        for (int rank = 0; rank < vectorSize; rank++) {
            BookSearchResponse book = vectorResults.get(rank);
            Long id = book.getId();
            if (id != null) {
                rrfScoreMap.merge(id, 1.0 / (K + rank + 1), Double::sum);
                bookMap.putIfAbsent(id, book);
            }
        }

        // 3. 점수 결합 및 리스트 생성 (초기 크기 할당)
        List<BookSearchResponse> result = new ArrayList<>(bookMap.size());
        for (Map.Entry<Long, BookSearchResponse> entry : bookMap.entrySet()) {
            Double score = rrfScoreMap.get(entry.getKey());
            result.add(entry.getValue().withRrfScore(score));
        }

        // 4. 내림차순 정렬 (래퍼 객체 생성 최소화)
        result.sort((b1, b2) -> Double.compare(
                b2.getRrfScore() != null ? b2.getRrfScore() : 0.0,
                b1.getRrfScore() != null ? b1.getRrfScore() : 0.0
        ));

        return result;
    }
}
