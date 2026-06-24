package com.nhnacademy.springailibrarycore.book.exception;

/**
 * 캐시 JSON 문자열을 도서 추천 결과 DTO 목록으로 디코딩(역직렬화)하는 중 발생하는 예외입니다.
 */
public class RecommendationCacheDecodeException extends RuntimeException {

    public RecommendationCacheDecodeException(String message, Throwable cause) {
        super(message, cause);
    }
}
