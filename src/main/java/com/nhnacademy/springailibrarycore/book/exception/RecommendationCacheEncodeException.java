package com.nhnacademy.springailibrarycore.book.exception;

/**
 * 도서 추천 결과를 JSON 캐시 문자열로 인코딩(직렬화)하는 중 발생하는 예외입니다.
 */
public class RecommendationCacheEncodeException extends RuntimeException {

    public RecommendationCacheEncodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
