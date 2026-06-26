package com.nhnacademy.springailibrarycore.library.dto.request;

import lombok.Builder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 인기대출 도서 조회 API (loanItemSrch) 호출을 위한 요청 DTO 클래스입니다.
 * <p>
 * 본 API는 전국의 도서관 빅데이터를 취합하여 특정 조건별 인기 도서 순위를 집계합니다.
 * 성별, 연령대, 행정구역, 주제 코드 등 검색 필터에 다중 선택(세미콜론 구분)을 지원합니다.
 * </p>
 *
 * @param startDt    검색 시작일자 (yyyy-MM-dd). 입력하지 않고 endDt도 없을 경우, 자동으로 현재 날짜 기준 한 달 전 날짜로 설정됩니다.
 * @param endDt      검색 종료일자 (yyyy-MM-dd). 입력하지 않고 startDt도 없을 경우, 자동으로 전날(어제) 날짜로 설정됩니다.
 * @param gender     성별 필터 코드. (예: "0"은 남성, "1"은 여성). 세미콜론(;)을 구분자로 사용하여 다중 선택이 가능합니다. (예: "0;1")
 * @param from_age   상세 연령 검색 조건의 시작 연령 (0 ~ 120 범위). age 파라미터와 병행하여 사용하지 않는 것이 권장됩니다.
 * @param to_age     상세 연령 검색 조건의 종료 연령 (0 ~ 120 범위). age 파라미터와 병행하여 사용하지 않는 것이 권장됩니다.
 * @param age        대표 연령대 필터 코드. 세미콜론(;)을 구분자로 다중 지정할 수 있습니다. (예: "20;30;40" -> 20대, 30대, 40대 동시 필터링)
 * @param region     행정구역 시도 코드. 세미콜론(;)을 구분자로 다중 지정할 수 있습니다. (예: "11;31" -> 서울, 경기)
 * @param dtl_region 행정구역 시군구 코드. 세미콜론(;)을 구분자로 다중 지정할 수 있습니다.
 * @param book_dvsn  도서 구분 필터. (예: "0"은 단행본, "1"은 비도서/기타)
 * @param addCode    ISBN 부가기호 코드 (1자리 또는 5자리). 세미콜론(;)을 구분자로 다중 지정할 수 있습니다. (예: "0;7;9")
 * @param kdc        KDC(한국십진분류법) 대분류 코드 (0~9). 세미콜론(;)을 구분자로 다중 지정할 수 있습니다. (예: "8;9" -> 문학, 역사)
 * @param dtl_kdc    KDC 세부 주제분류 코드 (소분류 수준). 세미콜론(;)을 구분자로 다중 지정할 수 있습니다.
 * @param pageNo     조회할 결과의 페이지 번호 (기본값: 1)
 * @param pageSize   한 페이지에 노출될 인기 도서의 개수 (기본값: 10)
 */
@Builder
public record PopularBooksSearchRequest(
        String startDt,
        String endDt,
        String gender,
        Integer from_age,
        Integer to_age,
        String age,
        String region,
        String dtl_region,
        String book_dvsn,
        String addCode,
        String kdc,
        String dtl_kdc,
        Integer pageNo,
        Integer pageSize
) {
    public PopularBooksSearchRequest {
        if (pageNo == null) pageNo = 1;
        if (pageSize == null) pageSize = 10;

        // startDt와 endDt가 모두 입력되지 않은 경우 (검색일자가 없는 경우) 최근 한 달간으로 설정
        if ((startDt == null || startDt.isBlank()) && (endDt == null || endDt.isBlank())) {
            LocalDate today = LocalDate.now();
            LocalDate oneMonthAgo = today.minusMonths(1);
            LocalDate yesterday = today.minusDays(1);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            startDt = oneMonthAgo.format(formatter);
            endDt = yesterday.format(formatter);
        }
    }

    /**
     * RestClient 요청 시 쿼리 파라미터 맵으로 변환해 주는 유틸 메서드입니다.
     * 
     * @return 쿼리 파라미터 Key-Value 맵
     */
    public MultiValueMap<String, String> toQueryParams() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        if (startDt != null && !startDt.isBlank()) params.add("startDt", startDt);
        if (endDt != null && !endDt.isBlank()) params.add("endDt", endDt);
        if (gender != null && !gender.isBlank()) params.add("gender", gender);
        if (from_age != null) params.add("from_age", String.valueOf(from_age));
        if (to_age != null) params.add("to_age", String.valueOf(to_age));
        if (age != null && !age.isBlank()) params.add("age", age);
        if (region != null && !region.isBlank()) params.add("region", region);
        if (dtl_region != null && !dtl_region.isBlank()) params.add("dtl_region", dtl_region);
        if (book_dvsn != null && !book_dvsn.isBlank()) params.add("book_dvsn", book_dvsn);
        if (addCode != null && !addCode.isBlank()) params.add("addCode", addCode);
        if (kdc != null && !kdc.isBlank()) params.add("kdc", kdc);
        if (dtl_kdc != null && !dtl_kdc.isBlank()) params.add("dtl_kdc", dtl_kdc);
        if (pageNo != null) params.add("pageNo", String.valueOf(pageNo));
        if (pageSize != null) params.add("pageSize", String.valueOf(pageSize));

        return params;
    }
}
