package com.nhnacademy.springailibrarycore.util;

import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Pageable 관련 공통 유틸리티 클래스입니다.
 *
 * DB 페이징을 지원하지 않는 검색(예: RRF 기반 하이브리드 검색)처럼
 * 인메모리 List를 Page로 변환해야 하는 경우에 사용합니다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PageableUtils {

    /**
     * 인메모리  List를 Pageable 정보를 기반으로 슬라이싱하여
     * Page로 변환합니다.
     *
     * @param list     전체 결과 목록
     * @param pageable 페이지 요청 정보 (pageNumber, pageSize, offset)
     * @param <T>      목록 요소 타입
     * @return 슬라이싱된 Page 객체
     */
    public static <T> Page<T> toPage(List<T> list, Pageable pageable) {
        int start = Math.toIntExact(pageable.getOffset());
        if (start >= list.size()) {
            return new PageImpl<>(List.of(), pageable, list.size());
        }
        int end = Math.min(start + pageable.getPageSize(), list.size());
        return new PageImpl<>(list.subList(start, end), pageable, list.size());
    }
}
