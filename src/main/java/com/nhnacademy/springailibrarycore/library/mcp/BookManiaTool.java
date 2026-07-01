package com.nhnacademy.springailibrarycore.library.mcp;

import com.nhnacademy.springailibrarycore.library.dto.common.NaruBookInfo;
import com.nhnacademy.springailibrarycore.library.service.agent.BookManiaCoordinator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class BookManiaTool {
    private final BookManiaCoordinator bookManiaCoordinator;

    @Tool(description = "특정 도서와 함께 많이 빌리는 마니아 추천 도서를 조회합니다. 추천유형(type)은 mania로 고정됩니다.")
    public String getManiaRecommendations(
            @ToolParam(description = "추천 기준이 되는 도서의 10자리 또는 13자리 ISBN입니다. 필수이며, 세미콜론(;)으로 구분해  최대 5개까지 입력 할 수 있습니다.")
            String isbn13
    ) {
        log.info("[Tool] getManiaRecommendations 호출: isbn13={}", isbn13);
        try {
            List<NaruBookInfo> books = bookManiaCoordinator.getManiaRecommendations(isbn13);
            if (books == null || books.isEmpty()) {
                return "마니아 추천 도서가 없습니다.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("마니아 추천 도서 검색 결과입니다. 총 %d권 중 최대 10권을 보여드립니다. %n",books.size()));

            books.stream().limit(10).forEach(book -> {
                sb.append(String.format("- %s", book.bookname()));

                if(book.authors() != null && !book.authors().isBlank()) {
                    sb.append(String.format(" / 저자: %s", book.authors()));
                }
                if(book.publisher() != null && !book.publisher().isBlank()) {
                    sb.append(String.format(" / 출판사: %s", book.publisher()));
                }
                if(book.publicationYear() != null && !book.publicationYear().isBlank()) {
                    sb.append(String.format(" / 출판년도: %s", book.publicationYear()));
                }
                if(book.isbn13() != null && !book.isbn13().isBlank()) {
                    sb.append(String.format(" / ISBN: %s", book.isbn13()));
                }
                if(book.additionSymbol() != null && !book.additionSymbol().isBlank()) {
                    sb.append(String.format(" / ISBN 부가기호: %s", book.additionSymbol()));
                }
                if(book.vol() != null && !book.vol().isBlank()) {
                    sb.append(String.format(" / 권: %s", book.vol()));
                }
                if(book.classNm() != null && !book.classNm().isBlank()) {
                    sb.append(String.format(" / 주제분류: %s", book.classNm()));
                }
                if(book.bookImageURL() != null && !book.bookImageURL().isBlank()) {
                    sb.append(String.format(" / 표지: %s", book.bookImageURL()));
                }
                sb.append(System.lineSeparator());
            });
            return sb.toString();
        } catch (Exception e) {
            log.error("[Tool] getManiaRecommendations 실패", e);
            return "마니아 추천 도서를 조회하는 중 오류가 발생했습니다.";
        }
    }

}
