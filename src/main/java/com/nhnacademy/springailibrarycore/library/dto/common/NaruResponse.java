package com.nhnacademy.springailibrarycore.library.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 도서관 정보나루 API 공통 응답 래퍼 클래스
 * 정보나루 API 응답 포맷 JSON은 루트 노드에 항상 {response}가 위치
 * 그 하위로 요청 파라미터 정보({@code request}) 및 실제 데이터 목록({@code docs} 혹은 {@code libs})이 반환
 *
 * @param <T> 실제 반환받고자 하는 데이터 구조의 타입 (예: NaruWrapper{@code <NaruBookInfo>})
 * @param response 최상위 response 객체
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaruResponse<T>(
        ResponseBody<T> response
) {
    /**
     * API 응답 본문 래퍼 레코드입니다.
     * 
     * @param <T> 데이터 타입
     * @param request 요청 조건 메타데이터 (페이지, 개수 등)
     * @param libs 도서관 정보 목록 (도서관 조회 API 시 제공됨)
     * @param docs 도서 정보 목록 (도서 조회, 인기도서, 추천도서 API 시 제공됨)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResponseBody<T>(
            RequestMetadata request,
            List<T> libs,
            List<T> docs
    ) {
        /**
         * API 종류에 따라 다르게 할당되는 데이터 목록({@code libs} 혹은 {@code docs})을 통합하여 일관되게 추출해 주는 도우미 메서드입니다.
         * 
         * @return 결과 리스트. 둘 다 존재하지 않거나 빈 경우 빈 리스트를 반환합니다.
         */
        public List<T> getItems() {
            if (docs != null) return docs;
            if (libs != null) return libs;
            return List.of();
        }
    }

    /**
     * API 요청 결과에 대한 메타데이터 레코드입니다.
     * 
     * @param pageNo     요청/반환된 페이지 번호
     * @param pageSize   한 페이지에 노출된 항목 수
     * @param numFound   전체 조건 만족 검색 건수
     * @param resultNum  현재 페이지에 반환된 실 결과 건수
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RequestMetadata(
            Integer pageNo,
            Integer pageSize,
            Integer numFound,
            Integer resultNum
    ) {}
}
