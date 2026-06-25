package com.nhnacademy.springailibrarycore.library.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 도서관 정보나루의 모든 도서 데이터에 대응하는 통합 도서 정보 DTO 레코드입니다.
 * <p>
 * 정보나루 API 마다 동일한 의미의 필드가 서로 다른 이름(예: 대출수 {@code loan_count} vs {@code loanCnt})으로 반환되거나,
 * 신착도서, 추천도서, 도서 검색, 상세 등에서 필드가 선별적으로 제공됩니다.
 * 이를 한 클래스 내에서 Jackson 매핑을 활용해 공통 모델화 하였습니다.
 * </p>
 *
 * @param no                 결과 목록 순번 (예: 마니아 추천 도서의 추천 인덱스)
 * @param ranking            인기 도서 대출 순위
 * @param bookname           도서 서명 (도서명)
 * @param authors            저자명
 * @param publisher          출판사명
 * @param publicationYear    출판년도 (yyyy)
 * @param publicationDate    출판일자 (yyyy-MM-dd)
 * @param isbn               10자리 ISBN
 * @param isbn13             13자리 ISBN
 * @param setIsbn13          세트 ISBN13
 * @param bookImageURL       책 표지 이미지 URL
 * @param bookDtlUrl         도서 상세 상세 페이지 URL
 * @param additionSymbol     ISBN 부가기호 (1자리 또는 5자리 코드)
 * @param vol                권차 정보
 * @param classNo            KDC 분류 기호 (주제 분류 번호)
 * @param classNm            KDC 분류 명칭 (주제 분류명)
 * @param description        도서 소개/설명글
 * @param loanCount          누적 대출 횟수 (인기 대출 도서 등에서 사용)
 * @param loanCnt            개별 조회 통계 대출 횟수
 * @param callNumbers        통합 청구기호 목록
 * @param callNumber         도서 단일 청구기호
 * @param separateShelfCode  별치기호 코드
 * @param separateShelfName  별치기호명
 * @param bookCode           도서 기호
 * @param shelfLocCode       배가기호 코드
 * @param shelfLocName       배가기호명
 * @param copyCode           복본기호 (복본 구분을 위한 코드)
 * @param regDate            등록일자 (신착도서 등 도서 등록일, yyyy-MM-dd)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NaruBookInfo(
        // 마니아 추천/상세 등 목록 번호/순위
        Integer no,
        String ranking,
        
        // 공통 도서 메타데이터
        String bookname,
        String authors,
        String publisher,
        @JsonProperty("publication_year") String publicationYear,
        @JsonProperty("publication_date") String publicationDate,
        String isbn,
        String isbn13,
        @JsonProperty("set_isbn13") String setIsbn13,
        String bookImageURL,
        @JsonProperty("bookDtlUrl") String bookDtlUrl,
        @JsonProperty("addition_symbol") String additionSymbol,
        String vol,
        @JsonProperty("class_no") String classNo,
        @JsonProperty("class_nm") String classNm,
        String description,
        
        // 대출 횟수
        @JsonProperty("loan_count") Integer loanCount,
        @JsonProperty("loanCnt") Integer loanCnt,
        
        // 도서관 소장 청구기호 정보
        String callNumbers,
        String callNumber,
        @JsonProperty("separate_shelf_code") String separateShelfCode,
        @JsonProperty("separate_shelf_name") String separateShelfName,
        @JsonProperty("book_code") String bookCode,
        @JsonProperty("shelf_loc_code") String shelfLocCode,
        @JsonProperty("shelf_loc_name") String shelfLocName,
        @JsonProperty("copy_code") String copyCode,
        @JsonProperty("reg_date") String regDate
) {}
