package com.nhnacademy.springailibrarycore.library.mcp;

import com.nhnacademy.springailibrarycore.library.dto.common.NaruBookInfo;
import com.nhnacademy.springailibrarycore.library.dto.request.PopularBooksSearchRequest;
import com.nhnacademy.springailibrarycore.library.service.agent.PopularBookSearchCoordinator;
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
public class PopularBookSearchTool {

    private final PopularBookSearchCoordinator popularBookSearchCoordinator;

    @Tool(
        description = "전국의 도서관 빅데이터를 기반으로 인기대출 도서 목록을 조회합니다. 자연어 조건(성별, 연령대, 대출기간, 지역, 주제분류 등)을 코드로 자동 변환하여 정밀한 인기 도서 순위를 제공합니다."
    )
    public String searchPopularBooks(
        @ToolParam(description = "대출 기간 (예: '최근 한 달', '최근 일주일', '2025년', '2026년 3월', 'yyyy-MM-dd부터 yyyy-MM-dd까지')", required = false) String dateRange,
        @ToolParam(description = "성별 (예: '남성', '여성')", required = false) String gender,
        @ToolParam(description = "대표 연령대 (예: '20대', '30대', '청소년', '대학생', '어린이')", required = false) String age,
        @ToolParam(description = "행정구역 시도명 (예: '서울', '경기도')", required = false) String region,
        @ToolParam(description = "행정구역 시군구명 (예: '강남구', '분당구')", required = false) String dtlRegion,
        @ToolParam(description = "도서 구분 (예: '큰글씨', '국외도서')", required = false) String bookDvsn,
        @ToolParam(description = "ISBN 부가기호 (예: '교양', '어린이도서', '학습서')", required = false) String addCode,
        @ToolParam(description = "KDC 대분류 주제 (예: '문학', '역사', '사회과학')", required = false) String kdc,
        @ToolParam(description = "KDC 소분류 상세주제 (예: '심리학', '경제학')", required = false) String dtlKdc,
        @ToolParam(description = "조회할 페이지 번호 (기본값: 1)", required = false) Integer pageNo,
        @ToolParam(description = "한 페이지에 표시할 도서 수 (기본값: 10)", required = false) Integer pageSize
    ) {
        log.info("[Tool] searchPopularBooks 호출: dateRange={}, gender={}, age={}, region={}, dtlRegion={}, bookDvsn={}, addCode={}, kdc={}, dtlKdc={}",
                dateRange, gender, age, region, dtlRegion, bookDvsn, addCode, kdc, dtlKdc);

        try {
            PopularBooksSearchRequest request = PopularBooksSearchRequest.builder()
                    .startDt(dateRange)
                    .gender(gender)
                    .age(age)
                    .region(region)
                    .dtlRegion(dtlRegion)
                    .bookDvsn(bookDvsn)
                    .addCode(addCode)
                    .kdc(kdc)
                    .dtlKdc(dtlKdc)
                    .pageNo(pageNo)
                    .pageSize(pageSize)
                    .build();

            List<NaruBookInfo> list = popularBookSearchCoordinator.search(request);

            if (list.isEmpty()) {
                return "조건에 만족하는 인기 도서 검색 결과가 없습니다.";
            }

            StringBuilder result = getResult(list);

            return result.toString();
        } catch (Exception e) {
            log.error("[Tool] searchPopularBooks 실패", e);
            return "인기 도서 목록을 조회하는 도중 예외가 발생했습니다: " + e.getMessage();
        }
    }

    @NotNull
    private static StringBuilder getResult(List<NaruBookInfo> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("📚 **인기 대출 도서 검색 결과:**\n\n");

        for (NaruBookInfo book : list) {
            // ranking이나 no 번호 노출
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
