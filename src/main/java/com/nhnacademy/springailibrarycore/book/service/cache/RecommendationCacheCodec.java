package com.nhnacademy.springailibrarycore.book.service.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.exception.RecommendationCacheDecodeException;
import com.nhnacademy.springailibrarycore.book.exception.RecommendationCacheEncodeException;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * AI 도서 추천 목록과 캐시 저장용 JSON 문자열 사이의 변환을 담당합니다.
 *
 * 데이터베이스의 {@code result} TEXT 컬럼에 추천 결과를 저장하거나,
 * 캐시 Hit 결과를 애플리케이션 DTO로 복원할 때 사용합니다.
 */
@Component
public class RecommendationCacheCodec {

    private static final TypeReference<List<BookSearchResponse>>
            RECOMMENDATION_LIST_TYPE = new TypeReference<>() {
            };

    private final ObjectMapper objectMapper;

    public RecommendationCacheCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(List<BookSearchResponse> recommendations) {
        try {
            // DTO -> JSON
            return objectMapper.writeValueAsString(recommendations);

        } catch (JsonProcessingException exception) {
            throw new RecommendationCacheEncodeException("추천 결과를 캐시 JSON으로 변환할 수 없습니다.", exception);
        }
    }

    public List<BookSearchResponse> decode(String result) {
        try {
            // JSON -> DTO
            if(result==null || result.isBlank()){
                return List.of();
            }
            return List.copyOf(objectMapper.readValue(result, RECOMMENDATION_LIST_TYPE));

        } catch (JsonProcessingException exception) {
            throw new RecommendationCacheDecodeException("캐시 JSON을 추천 결과로 변환할 수 없습니다.", exception);
        }
    }
}
