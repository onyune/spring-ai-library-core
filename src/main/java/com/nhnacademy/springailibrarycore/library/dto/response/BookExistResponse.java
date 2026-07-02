package com.nhnacademy.springailibrarycore.library.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 도서관별 도서 소장여부 및 대출 가능여부 조회 API 응답 DTO 클래스입니다.
 * 
 * @param response API 응답 데이터 본문
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookExistResponse(
        ResponseData response
) {
    /**
     * API 응답 데이터 레코드
     * 
     * @param result 소장 및 대출 가용성 결과 정보
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResponseData(
            ResultInfo result
    ) {}

    /**
     * 도서 소장 여부와 대출 가능 상태에 대한 세부 결과 레코드
     * 
     * @param hasBook       도서 소장 여부 ("Y": 소장 중, "N": 소장 안 함)
     * @param loanAvailable 현재 즉시 대출 가능 여부 ("Y": 대출 가능, "N": 대출 불가/대출 중)
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResultInfo(
            String hasBook,       // Y: 소장, N: 미소장
            String loanAvailable // Y: 대출가능, N: 대출불가
    ) {
        /**
         * 도서 소장 여부값을 boolean 형태로 반환
         * 
         * @return 소장 중이면 true, 그렇지 않으면 false
         */
        public boolean isHasBook() {
            return "Y".equalsIgnoreCase(hasBook);
        }

        /**
         * 현재 대출 가능 상태를 boolean 형태로 반환
         * 
         * @return 즉시 대출이 가능하면 true, 그렇지 않으면 false
         */
        public boolean isLoanAvailable() {
            return "Y".equalsIgnoreCase(loanAvailable);
        }
    }
}
