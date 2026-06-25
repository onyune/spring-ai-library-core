package com.nhnacademy.springailibrarycore.library.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 정보나루 API의 개별 요소 래핑 처리를 위한 유틸리티 제네릭 레코드입니다.
 * <p>
 * 정보나루 API는 JSON 응답 리스트 내부에서 각 행을 한번 더 객체로 감싸는 독특한 방식을 사용합니다.
 * (예: {@code [ { "doc": { "bookname": "..." } }, { "doc": { ... } } ]})
 * </p>
 * <p>
 * 본 클래스는 해당 JSON 바인딩 처리를 공통화하며, {@code doc}, {@code lib}, {@code book} 중 매핑되는 
 * 실제 비즈니스 데이터를 직접 꺼낼 수 있는 {@link #getContent()} 메서드를 제공합니다.
 * </p>
 *
 * @param <T> 실제 내부에 위치한 데이터 객체 타입 (예: NaruBookInfo, LibraryInfo)
 * @param lib 도서관 데이터가 담긴 키
 * @param doc 일반 서지/도서 데이터가 담긴 키
 * @param book 연관 추천도서 혹은 기타 데이터가 담긴 키
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaruWrapper<T>(
        T lib,
        T doc,
        T book
) {
    /**
     * 감싸진 내부 실 객체(Content)를 꺼내오는 헬퍼 메서드입니다.
     * 
     * @return 매핑된 실제 데이터 객체. 존재하지 않을 경우 null을 반환합니다.
     */
    public T getContent() {
        if (doc != null) return doc;
        if (lib != null) return lib;
        if (book != null) return book;
        return null;
    }
}
