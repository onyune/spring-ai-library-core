package com.nhnacademy.springailibrarycore.library.mcp;

import com.nhnacademy.springailibrarycore.library.dto.common.NaruBookInfo;
import com.nhnacademy.springailibrarycore.library.dto.response.NaruBookDetailResponse;
import com.nhnacademy.springailibrarycore.library.service.agent.BookDetailCoordinator;
import com.nhnacademy.springailibrarycore.telegram.tool.ToolResultContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookDetailTool {
    private final BookDetailCoordinator detailCoordinator;
    private final ToolResultContext toolResultContext;

    @Tool(description = "ISBN을 기준으로 도서의 상세 서지정보와 선택적인 대출 통계 정보를 조회합니다.")
    public String getBookDetail(
            @ToolParam(description = "조회할 도서의 10자리 또는 13자리 ISBN입니다.") String isbn13,
            @ToolParam(description = "대출 상세정보 제공 여부입니다. Y이면 대출 통계 정보를 포함하고, N이면 서지정보만 조횝합니다. 기본값 = N", required = false) String loaninfoYn,
            @ToolParam(description = "대출정보 조회 대상입니다. loanifoYN의 Y일때 사용하며 gender, age, region 중 하나를 입력합니다. 비워두면  전체 대출 정보를 조회합니다.", required = false) String displayInfo
    ) {
        log.info("[Tool] getBookDetail 호출 : isbn13={}, loanifoYN={}, displayInfo={}",
                isbn13, loaninfoYn, displayInfo);
        try {
            NaruBookDetailResponse.ResponseData detail =
                    detailCoordinator.getBookDetail(isbn13, loaninfoYn, displayInfo);
            if (detail == null || detail.detail() == null || detail.detail().book() == null) {
                return "FAIL: 도서 상세 정보를 찾을 수 없습니다.";
            }
            NaruBookInfo book = detail.detail().book();
            StringBuilder sb = new StringBuilder();
            sb.append("도서 상세 정보입니다.\n");
            sb.append(String.format("- 도서명: %s%n", book.bookname()));
            sb.append(String.format("- 저자: %s%n", book.authors()));
            sb.append(String.format("- 출판사: %s%n", book.publisher()));
            sb.append(String.format("- 출판년도: %s%n", book.publicationYear()));
            sb.append(String.format("- 출판일자: %s%n", book.publicationDate()));
            sb.append(String.format("- ISBN: %s%n", book.isbn()));
            sb.append(String.format("- ISBN13: %s%n", book.isbn13()));

            if (book.additionSymbol() != null && !book.additionSymbol().isBlank()) {
                sb.append(String.format("- ISBN 부가기호: %s%n", book.additionSymbol()));
            }
            if (book.vol() != null && !book.vol().isBlank()) {
                sb.append(String.format("- 권: %s%n", book.vol()));
            }
            if (book.classNo() != null && !book.classNo().isBlank()) {
                sb.append(String.format("- 주제 분류 번호: %s%n", book.classNo()));
            }
            if (book.classNm() != null && !book.classNm().isBlank()) {
                sb.append(String.format("- 주제 분류: %s%n", book.classNm()));
            }
            if (book.bookImageURL() != null && !book.bookImageURL().isBlank()) {
                sb.append(String.format("- 책표지 URL: %s%n", book.bookImageURL()));
            }
            if (book.description() != null && !book.description().isBlank()) {
                sb.append(String.format("- 책소개: %s%n", book.description()));
            }

            if (detail.loanInfo() != null) {
                sb.append("\n대출 통계 정보입니다.\n");
                if (detail.loanInfo().Total() != null) {
                    var total = detail.loanInfo().Total();
                    sb.append(String.format("- 전체 순위: %s / 대출 건수: %s%n", total.ranking(), total.loanCnt()));
                }
                if (detail.loanInfo().ageResult() != null && !detail.loanInfo().ageResult().isEmpty()) {
                    sb.append("- 연령별 대출정보\n");
                    detail.loanInfo().ageResult().forEach(wrapper -> {
                        var age = wrapper.getContent();
                        if (age != null) {
                            sb.append(String.format("  * %s: 순위 %s, 대출 %s건%n", age.name(), age.ranking(), age.loanCnt()));
                        }
                    });
                }
                if (detail.loanInfo().genderResult() != null && !detail.loanInfo().genderResult().isEmpty()) {
                    sb.append("- 성별 대출정보\n");
                    detail.loanInfo().genderResult().forEach(wrapper -> {
                        var gender = wrapper.getContent();
                        if (gender != null) {
                            sb.append(String.format("  * %s: 순위 %s, 대출 %s건%n", gender.name(), gender.ranking(), gender.loanCnt()));
                        }
                    });
                }
                if (detail.loanInfo().regionResult() != null && !detail.loanInfo().regionResult().isEmpty()) {
                    sb.append("- 지역별 대출정보\n");
                    detail.loanInfo().regionResult().forEach(wrapper -> {
                        var region = wrapper.getContent();
                        if (region != null) {
                            sb.append(String.format("  * %s: 순위 %s, 대출 %s건%n", region.name(), region.ranking(), region.loanCnt()));
                        }
                    });
                }
            }

            String report = sb.toString();

            // 실제 데이터를 RequestScope 컨텍스트에 임시 저장
            toolResultContext.addResult(report);

            return "SUCCESS: 도서 상세 정보 조회가 완료되었습니다.";

        } catch(Exception e) {
            log.error("[Tool] getBookDetail 실패", e);
            return "FAIL: 도서 상세 정보를 조회하는 중 오류가 발생했습니다.";
        }
    }
}
