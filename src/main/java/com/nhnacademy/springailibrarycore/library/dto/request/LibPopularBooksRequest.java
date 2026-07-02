package com.nhnacademy.springailibrarycore.library.dto.request;

import lombok.Builder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 도서관/지역별 인기대출 도서 조회 API (loanItemSrchByLib) 호출을 위한 요청 DTO 클래스
 * 필수 조건:{@code libCode}, {@code region}, {@code dtlRegion} 중 최소 하나는 반드시 제공되어야 합니다.
 * 특정 도서관 혹은 시도/시군구 단위 내의 대출 도서 순위를 분석할 수 있습니다.
 *
 * @param libCode    조회할 특정 도서관 코드
 * @param region     조회 대상 시도 코드 (예: "11"은 서울). 세미콜론(;) 구분자로 다중 지정 지원.
 * @param dtlRegion  조회 대상 시군구 코드. 세미콜론(;) 구분자로 다중 지정 지원.
 * @param startDt    검색 시작일자 (yyyy-MM-dd). 입력하지 않고 endDt도 없을 경우, 자동으로 현재 날짜 기준 한 달 전 날짜로 설정됩니다.
 * @param endDt      검색 종료일자 (yyyy-MM-dd). 입력하지 않고 startDt도 없을 경우, 자동으로 전날(어제) 날짜로 설정됩니다.
 * @param gender     성별 필터 코드. (예: "0"은 남성, "1"은 여성). 세미콜론(;)을 구분자로 사용하여 다중 선택이 가능합니다. (예: "0;1")
 * @param fromAge    상세 연령 검색 조건의 시작 연령 (0 ~ 120 범위)
 * @param toAge      상세 연령 검색 조건의 종료 연령 (0 ~ 120 범위)
 * @param age        대표 연령대 필터 코드. 세미콜론(;)을 구분자로 다중 지정할 수 있습니다. (예: "20;30")
 * @param addCode    ISBN 부가기호 코드. 세미콜론(;) 구분자로 다중 지정 지원.
 * @param kdc        KDC 대분류 코드 (0~9). 세미콜론(;) 구분자로 다중 지정 지원. (예: "8;9" -> 문학, 역사)
 * @param dtlKdc     KDC 세부 주제분류 코드. 세미콜론(;) 구분자로 다중 지정 지원.
 * @param bookDvsn   도서 구분 필터. (예: "0"은 단행본, "1"은 비도서/기타)
 * @param pageNo     조회할 결과의 페이지 번호 (기본값: 1)
 * @param pageSize   한 페이지에 노출될 결과 도서 수 (기본값: 10)
 */
@Builder
public record LibPopularBooksRequest(
        String libCode,
        String libName,
        String region,
        String dtlRegion,
        String startDt,
        String endDt,
        String gender,
        Integer fromAge,
        Integer toAge,
        String age,
        String addCode,
        String kdc,
        String dtlKdc,
        String bookDvsn,
        Integer pageNo,
        Integer pageSize
) {
    public LibPopularBooksRequest {
        if (pageNo == null) pageNo = 1;
        if (pageSize == null) pageSize = 10;

        // 검색 기간 미지정 시 최근 한달 자동 보정
        if ((startDt == null || startDt.isBlank()) && (endDt == null || endDt.isBlank())) {
            LocalDate today = LocalDate.now();
            LocalDate oneMonthAgo = today.minusMonths(1);
            LocalDate yesterday = today.minusDays(1);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            startDt = oneMonthAgo.format(formatter);
            endDt = yesterday.format(formatter);
        }
    }

    public MultiValueMap<String, String> toQueryParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        if (libCode != null && !libCode.isBlank()) params.add("libCode", libCode);
        if (region != null && !region.isBlank()) params.add("region", region);
        if (dtlRegion != null && !dtlRegion.isBlank()) params.add("dtl_region", dtlRegion);
        if (startDt != null && !startDt.isBlank()) params.add("startDt", startDt);
        if (endDt != null && !endDt.isBlank()) params.add("endDt", endDt);
        if (gender != null && !gender.isBlank()) params.add("gender", gender);
        if (fromAge != null) params.add("from_age", String.valueOf(fromAge));
        if (toAge != null) params.add("to_age", String.valueOf(toAge));
        if (age != null && !age.isBlank()) params.add("age", age);
        if (addCode != null && !addCode.isBlank()) params.add("addCode", addCode);
        if (kdc != null && !kdc.isBlank()) params.add("kdc", kdc);
        if (dtlKdc != null && !dtlKdc.isBlank()) params.add("dtl_kdc", dtlKdc);
        if (bookDvsn != null && !bookDvsn.isBlank()) params.add("book_dvsn", bookDvsn);
        if (pageNo != null) params.add("pageNo", String.valueOf(pageNo));
        if (pageSize != null) params.add("pageSize", String.valueOf(pageSize));

        return params;
    }
}
