package com.nhnacademy.springailibrarycore.telegram.agent;

import com.nhnacademy.springailibrarycore.library.mcp.*;
import com.nhnacademy.springailibrarycore.telegram.dto.AskRequest;
import com.nhnacademy.springailibrarycore.telegram.dto.AskResponse;
import com.nhnacademy.springailibrarycore.telegram.tool.BookSearchTool;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 텔레그램 모듈의 단일 진입점으로부터 요청을 받아 Tool을 활용해 사용자에게 최종 답변을 생성하는 만능 사서 에이전트.
 */
@Service
@Slf4j
public class AiLibraryAssistantAgent {

    private final ChatClient chatClient;

    public AiLibraryAssistantAgent(
            @Qualifier("geminiChatClientBuilder") ChatClient.Builder chatClientBuilder,
            PopularBookSearchTool popularBookSearchTool,
            LibrarySearchTool librarySearchTool,
            BookSearchTool bookSearchTool,
            MultipleBookLoanAvailabilityTool multipleBookLoanAvailabilityTool,
            BookManiaTool bookManiaTool,
            BookDetailTool bookDetailTool,
            BookUsageAnalysisTool bookUsageAnalysisTool,
            MonthlyKeywordTool monthlyKeywordTool,
            NewArrivalBookTool newArrivalBookTool,
            LibPopularBookTool libPopularBookTool
    ) {
        this.chatClient = chatClientBuilder.defaultSystem("""
                        당신은 NHN 도서관의 친절하고 전문적인 AI 사서입니다.
                        사용자의 질문에 답변하기 위해 제공된 도구(Tools)들을 적극적으로 활용하세요.
                        
                        [중요: 응답 형식 및 양식 규칙]
                        응답은 반드시 JSON 배열 형태로 작성해야 합니다. 사용자의 질문에 따라 여러 개의 응답(배열 항목)을 반환할 수 있습니다.
                        
                        1. 도서 검색/추천 정보인 경우:
                           - 해당 도서의 ID를 `bookId` 필드에 넣으세요.
                           - `answer` 필드는 반드시 아래 마크다운 양식을 엄격하게 지켜 작성하세요:
                             **[title]**
                             저자: [authorName]
                             출판사: [publisherName]
                             연관도: [relevance]% (relevance가 있는 경우만 출력)
                        
                             추천 사유:
                             [aiComment 또는 도구를 기반으로 생성한 답변 내용]
                        
                        2. 도서관 일반 정보(이용 시간, 위치 등)나 일상 대화인 경우:
                           - `bookId` 필드를 `null`로 설정하세요.
                           - `answer` 필드에 친절하고 가독성 좋은 일반 마크다운 텍스트로 자유롭게 답변을 작성하세요.
                        
                        3. ⚠️ 두 가지를 동시에 물어본 경우:
                           - 일반 정보 답변 항목(bookId: null)과 도서 추천 답변 항목(bookId: 숫자)들을 하나의 배열에 모두 포함해서 반환하세요.
                        """)
                .defaultTools(popularBookSearchTool,
                        librarySearchTool,
                        bookSearchTool,
                        multipleBookLoanAvailabilityTool,
                        bookManiaTool,
                        bookDetailTool,
                        bookUsageAnalysisTool,
                        monthlyKeywordTool,
                        newArrivalBookTool,
                        libPopularBookTool)
                .build();
    }

    @Transactional
    public List<AskResponse> ask(AskRequest request) {
        try {
            String promptText = "사용자의 질문: %s%n%n(시스템 제공 정보: 현재 질문한 사용자의 chatId는 %d 입니다. 도서 검색 도구를 호출할 때 이 chatId를 파라미터로 반드시 전달해주세요.)"
                    .formatted(request.question(), request.chatId());

            List<AskResponse> aiResponses = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .entity(new ParameterizedTypeReference<List<AskResponse>>() {
                    });

            log.info("[Assistant] 최종 생성된 응답 갯수: {}", aiResponses.size());
            aiResponses.forEach(r -> log.info(" - bookId: {}, answer: {}", r.bookId(), r.answer()));

            return aiResponses;

        } catch (Exception e) {
            log.error("[Assistant] 답변 생성 중 에러 발생", e);
            return List.of(new AskResponse(null, "죄송합니다. 요청을 처리하는 중 문제가 발생했습니다."));
        }
    }
}
