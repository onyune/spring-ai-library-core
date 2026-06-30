package com.nhnacademy.springailibrarycore.library.mcp;

import com.nhnacademy.springailibrarycore.library.dto.common.NaruBookInfo;
import com.nhnacademy.springailibrarycore.library.dto.common.NaruWrapper;
import com.nhnacademy.springailibrarycore.library.dto.request.LibPopularBooksRequest;
import com.nhnacademy.springailibrarycore.library.dto.response.LibPopularBooksResponse;
import com.nhnacademy.springailibrarycore.library.service.agent.LibPopularBookCoordinator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LibPopularBookTool {

    private final LibPopularBookCoordinator libPopularBookCoordinator;

    @Tool(
            description = "특정 도서관 또는 지역 기준 인기대출 도서 목록을 조회합니다. 도서관명, 도서관 코드, 지역, 성별, 연령대, 주제분류 등의 조건을 코드로 변환하여 도서관/지역별 대출 순위를 제공합니다."
    )
    public String libPopularBooks(
            @ToolParam(description = "조회 대상 6자리 도서관 코드 (도서관 코드를 명확히 아는 경우 직접 지정)", required = false) String libCode,
            @ToolParam(description = "검색하거나 상세 정보를 확인할 도서관 이름 (예: 강남도서관, 마포평생학습관)", required = false) String libName,
            @ToolParam(description = "행정구역 시도명 (예: '서울', '경기도')", required = false) String region,
            @ToolParam(description = "행정구역 시군구명 (예: '강남구', '분당구')", required = false) String dtlRegion,
            @ToolParam(description = "대출 기간 (예: '최근 한 달', '최근 일주일', '2025년', '2026년 3월', 'yyyy-MM-dd부터 yyyy-MM-dd까지')", required = false) String dateRange,
            @ToolParam(description = "성별 (예: '남성', '여성')", required = false) String gender,
            @ToolParam(description = "대표 연령대 (예: '20대', '30대', '청소년', '대학생', '어린이')", required = false) String age,
            @ToolParam(description = "ISBN 부가기호 (예: '교양', '어린이도서', '학습서')", required = false) String addCode,
            @ToolParam(description = "KDC 대분류 주제 (예: '문학', '역사', '사회과학')", required = false) String kdc,
            @ToolParam(description = "KDC 소분류 상세주제 (예: '심리학', '경제학')", required = false) String dtlKdc,
            @ToolParam(description = "도서 구분 (예: '큰글씨', '국외도서')", required = false) String bookDvsn,
            @ToolParam(description = "조회할 페이지 번호 (기본값: 1)", required = false) Integer pageNo,
            @ToolParam(description = "한 페이지에 표시할 도서 수 (기본값: 10)", required = false) Integer pageSize
    ){

        log.info("[Tool] LibPopularBooks 호출: libCode={},libName={}, dateRange={}, gender={}, age={}, region={}, dtlRegion={}, bookDvsn={}, addCode={}, kdc={}, dtlKdc={}",
                libCode,libName,dateRange, gender, age, region, dtlRegion, bookDvsn, addCode, kdc, dtlKdc);

        try{
            LibPopularBooksRequest request = LibPopularBooksRequest.builder()
                    .libCode(libCode)
                    .libName(libName)
                    .region(region)
                    .dtlRegion(dtlRegion)
                    .startDt(dateRange)
                    .gender(gender)
                    .age(age)
                    .addCode(addCode)
                    .kdc(kdc)
                    .dtlKdc(dtlKdc)
                    .bookDvsn(bookDvsn)
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .build();

            LibPopularBooksResponse.ResponseData response = libPopularBookCoordinator.search(request);
            List<NaruBookInfo> list = response != null && response.docs() != null
                    ? response.docs().stream()
                    .map(NaruWrapper::getContent)
                    .toList()
                    : List.of();

            if(list.isEmpty()){
                return "조건에 만족하는 도서관 / 지역별 인기 도서가 없습니다.";
            }

            StringBuilder result = getLibResult(response, list);

            return result.toString();

        }catch (Exception e){
            log.error("[Tool] libPopularBooks 실패", e);
            return "도서관 / 지역별 인기 도서 목록을 조회하는 도중 예외가 발생했습니다: " +  e.getMessage();
        }
    }

    @NotNull
    private static StringBuilder getLibResult(LibPopularBooksResponse.ResponseData response, List<NaruBookInfo> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("📚 **도서관 / 지역별 인기 도서 목록 검색 결과:**\n\n");

        if (response != null) {
            if (response.libNm() != null && !response.libNm().isBlank()) {
                sb.append("도서관 이름: ").append(response.libNm()).append("\n");
            }

            if (response.regionNm() != null && !response.regionNm().isBlank()) {
                sb.append("지역: ").append(response.regionNm()).append("\n");
            }

            if (response.dtlregionNm() != null && !response.dtlregionNm().isBlank()) {
                sb.append("세부지역: ").append(response.dtlregionNm()).append("\n");
            }

            sb.append("\n");
        }

        for (NaruBookInfo book : list) {
            String rank = book.ranking() != null ? book.ranking() : (book.no() != null ? String.valueOf(book.no()) : "-");
            Integer loans = book.loanCount() != null ? book.loanCount() : (book.loanCnt() != null ? book.loanCnt() : 0);

            sb.append(String.format("%s. **%s** (저자: %s | 출판사: %s)\n", rank, book.bookname(), book.authors(), book.publisher()));
            sb.append(String.format("   * 대출 순위: **%s위** (대출 건수: %d건)\n", rank, loans));
            if (book.publicationYear() != null && !book.publicationYear().isBlank()) {
                sb.append(String.format("   * 발행년도: %s년\n", book.publicationYear()));
            }
            if (book.classNm() != null && !book.classNm().isBlank()) {
                sb.append(String.format("   * 분류주제: %s (%s)\n", book.classNm(), book.classNo()));
            } else if (book.classNo() != null && !book.classNo().isBlank()) {
                sb.append(String.format("   * 분류기호: %s\n", book.classNo()));
            }
            sb.append("\n");
        }

        return sb;
    }
}
