package com.nhnacademy.springailibrarycore.dto;

import org.springframework.data.domain.Page;

/**
 * 검색 파이프라인의 기본 결과 DTO.
 *
 * <p>AI 추천 결과는 RAG 단계에서 필요해질 때 확장한다.</p>
 */
public record BookSearchResult(Page<BookSearchResponse> books) {
}
