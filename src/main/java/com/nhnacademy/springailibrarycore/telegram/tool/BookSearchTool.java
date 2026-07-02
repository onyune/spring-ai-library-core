package com.nhnacademy.springailibrarycore.telegram.tool;

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

import com.nhnacademy.springailibrarycore.book.service.preference.PersonalizedRecommendationService;

@RequiredArgsConstructor
@Slf4j
@Component
public class BookSearchTool {
    private final BookSearchAgent bookSearchAgent;
    private final ToolResultContext toolResultContext;
    private final PersonalizedRecommendationService personalizedRecommendationService;

    @Tool(description = "도서관 시스템에서 도서를 추천받거나 검색합니다. AI 연관성 점수(relevance)이 제일 높은 책 3개만 반환합니다.")
    public String searchBooks(
            @ToolParam(description = "사용자의 도서 추천 및 검색 의도인 부분만 그대로 발췌합니다.") String query,
            @ToolParam(description = "사용자의 채팅방 ID (chatId)") Long chatId
    ) {
        log.info("[Tool] searchBooks 호출: query={}, chatId={}", query, chatId);

        // SearchType.RAG로 요청 생성
        BookSearchRequest request = new BookSearchRequest(query, null, SearchType.RAG, null, chatId);

        // BookSearchAgent 실행
        BookSearchResult result = bookSearchAgent.searchBooks(PageRequest.of(0, 3), request);
        List<BookSearchResponse> books = result.books().getContent().stream()
                .limit(3)
                .toList();

        // 실제 데이터를 RequestScope 컨텍스트에 임시 저장
        toolResultContext.addResult(books);

        // AI가 최종 추천한 책들(aiComment가 달린 도서)만 필터링하여 ISBN 목록 추출
        List<String> recommendedIsbns = books.stream()
                .filter(book -> book.getAiComment() != null && !book.getAiComment().isBlank() && !"-".equals(book.getAiComment()))
                .map(BookSearchResponse::getIsbn)
                .toList();

        // LLM에게 추천된 도서 ISBN 목록을 포함하여 반환 (체이닝 유도)
        return "SUCCESS: 도서 추천 완료. 추천 도서 ISBN 목록: " + recommendedIsbns;
    }

    @Tool(description = "사용자가 '내가 좋아할만한 책', '나한테 맞는 책' 등 개인화된 맞춤형 추천을 원할 때 반드시 이 도구를 사용하세요. 사용자의 과거 독서 취향을 분석하여 딱 맞는 책 3권을 추천합니다.")
    public String getPersonalizedRecommendation(
            @ToolParam(description = "사용자의 채팅방 ID (chatId)") Long chatId
    ) {
        log.info("[Tool] getPersonalizedRecommendation 호출: chatId={}", chatId);

        BookSearchResult result = personalizedRecommendationService.getPersonalizedRecommendationBooks(chatId);
        List<BookSearchResponse> books = result.books().getContent().stream()
                .map(book -> book.toBuilder().aiComment("사용자 선호책 탐색").build())
                .toList();

        log.info("[BookSearchTool - getPersonalizedRecommendation] books Size:{}", books.size());
        toolResultContext.addResult(books);

        List<String> recommendedIsbns = books.stream()
                .map(BookSearchResponse::getIsbn)
                .toList();

        return "SUCCESS: 개인화 맞춤 도서 추천 완료. 사용자 취향에 가장 가까운 3권의 ISBN 목록입니다: " + recommendedIsbns;
    }
}
