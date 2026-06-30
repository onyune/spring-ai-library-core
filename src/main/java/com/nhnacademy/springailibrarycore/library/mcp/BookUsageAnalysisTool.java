package com.nhnacademy.springailibrarycore.library.mcp;

import com.nhnacademy.springailibrarycore.library.dto.common.NaruBookInfo;
import com.nhnacademy.springailibrarycore.library.dto.response.NaruBookUsageAnalysisResponse;
import com.nhnacademy.springailibrarycore.library.service.agent.BookUsageAnalysisCoordinator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookUsageAnalysisTool {
    private final BookUsageAnalysisCoordinator bookUsageAnalysisCoordinator;

    @Tool(description = "ISBN을 기준으로 도서의 이용 분석 정보를 조회합니다. 서지정보, 대출 추이, 주 이용자 그룹, 핵심 키워드, 동시대출 도서, 추천도서를 제공합니다.")
    public String getBookUsageAnalysis(
            @ToolParam(description = "분석할 도서의 10자리 또는 13자리 ISBN입니다.") String isbn13
    ) {
        log.info("[Tool] getBookUsageAnalysis 호출: isbn13={}", isbn13);
        try {
            NaruBookUsageAnalysisResponse.ResponseData analysis = bookUsageAnalysisCoordinator.getBookUsageAnalysis(isbn13);
            if (analysis == null || analysis.book() == null){
                return "도서 이용 분석 정보를 찾을 수 없습니다.";
            }
            NaruBookInfo book = analysis.book();

            StringBuilder sb = new StringBuilder();
            sb.append("도서 이용 분석 정보입니다.\n");
            sb.append(String.format("- 도서명: %s%n", book.bookname()));
            sb.append(String.format("- 저자: %s%n", book.authors()));
            sb.append(String.format("- 출판사: %s%n", book.publisher()));
            sb.append(String.format("- 출판년도: %s%n", book.publicationYear()));
            sb.append(String.format("- ISBN: %s%n", book.isbn13()));

            if (book.loanCnt() != null) {
                sb.append(String.format("- 대출 건수: %d%n", book.loanCnt()));
            }
            if (analysis.loanHistory() != null && !analysis.loanHistory().isEmpty()) {
                sb.append("\n최근 대출 추이입니다.\n");
                analysis.loanHistory().stream().limit(5).forEach(wrapper -> {
                    var loan = wrapper.loan();
                    if (loan != null) {
                        sb.append(String.format("- %s: 대출 %d건, 순위 %d%n",
                                loan.month(), loan.loanCnt(), loan.ranking()));
                    }
                });
            }
            if (analysis.loanGrps() != null && !analysis.loanGrps().isEmpty()) {
                sb.append("\n주 이용자 그룹입니다.\n");
                analysis.loanGrps().stream().limit(10).forEach(wrapper -> {
                    var group = wrapper.loanGrp();
                    if (group != null) {
                        sb.append(String.format("- %s %s: 대출 %d건, 순위 %d%n",
                                group.age(), group.gender(), group.loanCnt(), group.ranking()));
                    }
                });
            }
            if (analysis.keywords() != null && !analysis.keywords().isEmpty()) {
                sb.append("\n핵심 키워드입니다.\n");
                analysis.keywords().stream().limit(10).forEach(wrapper -> {
                    var keyword = wrapper.keyword();
                    if (keyword != null) {
                        sb.append(String.format("- %s (가중치: %.2f)%n",
                                keyword.word(), keyword.weight()));
                    }
                });
            }
            if (analysis.coLoanBooks() != null && !analysis.coLoanBooks().isEmpty()) {
                sb.append("\n함께 대출된 도서입니다.\n");
                analysis.coLoanBooks().stream().limit(5).forEach(wrapper -> {
                    NaruBookInfo coLoanBook = wrapper.book();
                    if (coLoanBook != null) {
                        sb.append(String.format("- %s / 저자: %s / ISBN: %s%n",
                                coLoanBook.bookname(), coLoanBook.authors(), coLoanBook.isbn13()));
                    }
                });
            }
            if (analysis.maniaRecBooks() != null && !analysis.maniaRecBooks().isEmpty()) {
                sb.append("\n마니아 추천 도서입니다.\n");
                analysis.maniaRecBooks().stream().limit(5).forEach(wrapper -> {
                    NaruBookInfo recBook = wrapper.book();
                    if (recBook != null) {
                        sb.append(String.format("- %s / 저자: %s / ISBN: %s%n",
                                recBook.bookname(), recBook.authors(), recBook.isbn13()));
                    }
                });
            }
            if (analysis.readerRecBooks() != null && !analysis.readerRecBooks().isEmpty()) {
                sb.append("\n독자 추천 도서입니다.\n");
                analysis.readerRecBooks().stream().limit(5).forEach(wrapper -> {
                    NaruBookInfo recBook = wrapper.book();
                    if (recBook != null) {
                        sb.append(String.format("- %s / 저자: %s / ISBN: %s%n",
                                recBook.bookname(), recBook.authors(), recBook.isbn13()));
                    }
                });
            }
            return sb.toString();
        } catch(Exception e) {
            log.error("[Tool] getBookUsageAnalysis 실패", e);
            return "도서 이용 분석 정보를 조회하는 중 오류가 발생했습니다.";
        }
    }
}
