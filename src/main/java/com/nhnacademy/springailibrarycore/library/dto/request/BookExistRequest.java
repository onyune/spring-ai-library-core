package com.nhnacademy.springailibrarycore.library.dto.request;

import lombok.Builder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 도서관별 도서 소장여부 및 대출 가능여부 조회 API (bookExist) 호출을 위한 요청 DTO 클래스
 * 특정 도서관({@code libCode})에 특정 도서({@code isbn13})가 소장되어 있는지와 현재 즉시 대출이 가능한 지 여부를 조회
 *
 * @param libCode 대상 도서관의 10자리 도서관 코드 (필수)
 * @param isbn13  확인할 도서의 13자리 ISBN 번호 (필수)
 */
@Builder
public record BookExistRequest(
        String libCode,
        String libName,
        String isbn13,
        String bookTitle
) {

    public MultiValueMap<String, String> toQueryParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        if (libCode != null && !libCode.isBlank()) params.add("libCode", libCode);
        if (isbn13 != null && !isbn13.isBlank()) params.add("isbn13", isbn13);
        return params;
    }
}
