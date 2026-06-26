package com.nhnacademy.springailibrarycore.library.mcp;

import com.nhnacademy.springailibrarycore.agent.BookSearchAgent;
import com.nhnacademy.springailibrarycore.book.domain.SearchType;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class BookSearchTool {
    private final BookSearchAgent bookSearchAgent;

    @Tool(description = "도서관 시스템에서 도서를 검색합니다. (최대 3권)")
    public List<BookSearchResponse> searchBooks(
            @ToolParam(description = "도서 검색 키워드") String query
    ) {
        // SearchType.AUTO로 요청 생성
        BookSearchRequest request = new BookSearchRequest(query, null, SearchType.AUTO, null, false);

        // BookSearchAgent
        BookSearchResult result = bookSearchAgent.searchBooks(PageRequest.of(0, 3), request);

        return result.books().getContent();
    }
}
