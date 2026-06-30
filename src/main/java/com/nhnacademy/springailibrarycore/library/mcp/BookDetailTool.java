package com.nhnacademy.springailibrarycore.library.mcp;

import com.nhnacademy.springailibrarycore.library.dto.common.NaruBookInfo;
import com.nhnacademy.springailibrarycore.library.dto.response.NaruBookDetailResponse;
import com.nhnacademy.springailibrarycore.library.service.agent.BookDetailCoordinator;
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
                return "도서 상세 정보를 찾을 수 없습니다.";
            }
            NaruBookInfo book = detail.detail().book();
            StringBuilder sb = new StringBuilder();
            sb.append("도서 상세 정보입니다.\n");
            sb.append(String.format("- 도서명: %s%n", book.bookname()));
            sb.append(String.format("- 저자: %s%n", book.authors()));
            sb.append(String.format("- 출판사: %s%n", book.publisher()));
            sb.append(String.format("- 출판년도: %s%n", book.publicationYear()));
            sb.append(String.format("- ISBN: %s%n", book.isbn13()));

            if (book.classNm() != null && !book.classNm().isBlank()) {
                sb.append(String.format("- 주제 분류: %s%n", book.classNm()));
            }
            if (book.description() != null && !book.description().isBlank()) {
                sb.append(String.format("- 책소개: %s%n", book.description()));
            }

            if (detail.loanInfo() != null && detail.loanInfo().Total() != null) {
                var total = detail.loanInfo().Total();
                sb.append("\n대출 통계 정보입니다.\n");
                sb.append(String.format("- 전체 순위: %s%n", total.ranking()));
                sb.append(String.format("- 대출 건수: %s%n", total.loanCnt()));
            }

            return sb.toString();
        } catch(Exception e) {
            log.error("[Tool] getBookDetail 실패", e);
            return "도서 상세 정보를 조회하는 중 오류가 발생했습니다.";
        }
    }
}
