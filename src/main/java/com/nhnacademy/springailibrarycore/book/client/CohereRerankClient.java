package com.nhnacademy.springailibrarycore.book.client;


import com.nhnacademy.springailibrarycore.book.dto.cohere.CohereRerankRequest;
import com.nhnacademy.springailibrarycore.book.dto.cohere.CohereRerankResponse;
import com.nhnacademy.springailibrarycore.book.dto.cohere.CohereRerankResult;
import java.util.List;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cohere Rerank API를 호출하여 도서들의 문맥적 유사도를 정밀 채점(Cross-Encoder)하는 클라이언트입니다.
 *
 * 내부 동작 원리 (Cross-Encoder 방식):
 *
 * - 기존 벡터 검색(Bi-Encoder)이 질문과 문서를 각각 임베딩하여 거리를 비교하는 방식이라면,
 * - 이 클라이언트는 "질문 텍스트"와 "도서 텍스트"를 동시에 하나의 AI모델(Transformer)에 넣고 읽게 만듭니다.
 * - 이를 통해 단순 단어 일치가 아닌, 두 문장 간의 깊은 문맥적(Semantic) 유사도를 0.0 ~ 1.0 사이의 정확도 점수로 계산하여 가장 적합한 도서를 재정렬(Re-sorting)해 줍니다.
 */
@Slf4j
@Component
public class CohereRerankClient {
    @Value("${cohere.api.key}")
    private String apiKey;

    @Value("${cohere.api.url}")
    private String url;

    @Value("${cohere.api.rank.k}")
    private Integer rerank_k;


    private final RestClient restClient = RestClient.create();

    public List<Integer> rerank(String query, List<String> documents) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        try {
            CohereRerankRequest requestBody = new CohereRerankRequest(
                    "rerank-multilingual-v3.0",
                    query,
                    documents,
                    rerank_k // 상위 20개만 반환 요청
            );

            CohereRerankResponse response = restClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(CohereRerankResponse.class);

            if (response == null || response.results() == null) {
                return List.of();
            }

            // 가장 관련성 높은 순서대로 정렬되어 내려온 results에서 원본 index만 추출
            return response.results().stream()
                    .map(CohereRerankResult::index)
                    .toList();

        } catch (Exception e) {
            log.error("[CohereRerankClient] Cohere API 호출 중 에러 발생: {}", e.getMessage());
            // 장애 발생 시 Fallback: 원래 순서대로 상위 20개만 반환
            return IntStream.range(0, Math.min(20, documents.size()))
                    .boxed()
                    .toList();
        }
    }


}
