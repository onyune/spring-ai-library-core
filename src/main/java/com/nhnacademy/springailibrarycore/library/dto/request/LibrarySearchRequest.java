package com.nhnacademy.springailibrarycore.library.dto.request;

import lombok.Builder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 정보공개 도서관 조회 API(libSrch) 호출을 위한 요청 DTO 클래스
 * 
 * @param libCode     특정 도서관을 지정하여 단일 조회할 때 사용하는 도서관 코드 (생략 시 전체 조회)
 * @param region      조회 대상 지역 대분류 코드 (시/도 코드 2자리, 생략 시 전체 지역 대상)
 * @param dtlRegion   조회 대상 지역 소분류 코드 (시/군/구 코드 3자리, 생략 시 전체 대상)
 * @param pageNo      조회할 결과의 페이지 번호 (기본값: 1)
 * @param pageSize    한 페이지에 노출될 도서관 목록의 크기 (기본값: 10)
 */
@Builder
public record LibrarySearchRequest(
        String libCode,
        Integer region,
        Integer dtlRegion,
        Integer pageNo,
        Integer pageSize
) {
    public LibrarySearchRequest {
        if (pageNo == null) pageNo = 1;
        if (pageSize == null) pageSize = 10;
    }

    public MultiValueMap<String, String> toQueryParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        if (libCode != null && !libCode.isBlank()) params.add("libCode", libCode);
        if (region != null) params.add("region", String.valueOf(region));
        if (dtlRegion != null) params.add("dtl_region", String.valueOf(dtlRegion));
        if (pageNo != null) params.add("pageNo", String.valueOf(pageNo));
        if (pageSize != null) params.add("pageSize", String.valueOf(pageSize));

        return params;
    }
}