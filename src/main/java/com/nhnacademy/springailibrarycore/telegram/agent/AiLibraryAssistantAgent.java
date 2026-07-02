package com.nhnacademy.springailibrarycore.telegram.agent;

import com.nhnacademy.springailibrarycore.book.dto.BookSearchResponse;
import com.nhnacademy.springailibrarycore.library.mcp.*;
import com.nhnacademy.springailibrarycore.telegram.dto.AskRequest;
import com.nhnacademy.springailibrarycore.telegram.dto.AskResponse;
import com.nhnacademy.springailibrarycore.telegram.tool.BookSearchTool;
import com.nhnacademy.springailibrarycore.telegram.tool.ToolResultContext;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 텔레그램 모듈의 단일 진입점으로부터 요청을 받아 Tool을 활용해 사용자에게 최종 답변을 생성하는 만능 사서 에이전트.
 */
@Service
@Slf4j
public class AiLibraryAssistantAgent {

    private final ChatClient chatClient;
    private final ToolResultContext toolResultContext;

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
            LibPopularBookTool libPopularBookTool,
            ToolResultContext toolResultContext
    ) {
        this.toolResultContext = toolResultContext;
        this.chatClient = chatClientBuilder.defaultSystem("""
                        당신은 NHN 도서관의 친절하고 전문적인 AI 사서입니다.
                        [규칙]
                        1. 임의로 답변을 지어내지 말고, 도서 검색/추천/대출/도서관 조회 등은 반드시 제공된 도구(Tools)를 호출해 대답하십시오.
                        2. 도구 결과로 반환된 식별자(ISBN 목록, 도서관 코드 등)가 다음 작업에 필요한 경우, 해당 식별자들을 다음 도구의 입력 파라미터로 넘겨 연쇄적으로 도구를 실행(Chaining)하십시오.
                        3. '빌릴 수 있는 도서관', '대출 가능한 곳' 등의 질문은 항상 특정 도서들(앞서 추천/검색된 도서 등)의 실제 대출 여부를 조회하려는 목적입니다. 이때 단순 도서관 검색 도구(LibrarySearchTool)를 호출하지 말고, 반드시 대출 조회 도구(MultipleBookLoanAvailabilityTool)를 호출하여 대상 도서 정보(ISBN)를 파라미터로 넘기십시오.
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

            // AI 1차 툴 실행 루프 가동 및 플레인 텍스트 결과 수신 (JSON 변환 스키마 전송 생략)
            String aiResponseText = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .content();

            // RequestScope 컨텍스트에 모인 원시 툴 실행 결과 획득
            List<Object> toolResults = toolResultContext.getResults();
            log.info("[Assistant] toolResultContext 수집 결과 개수: {}", toolResults.size());

            // 실행된 도구가 없는 경우 (일상 대화 등) ➔ AI 텍스트 그대로 DTO 조립
            if (toolResults.isEmpty()) {
                log.info("[Assistant] 실행된 도구 없음 -> 일반 대화 응답 반환");
                return List.of(new AskResponse(null, aiResponseText));
            }

            // 도구 데이터가 저장된 경우 ➔ 자바 코드에서 수동 조립
            List<AskResponse> responses = new ArrayList<>();
            for (Object result : toolResults) {
                if (result instanceof List) {
                    // 도서 추천/검색 결과 리스트인 경우 (BookSearchResponse)
                    @SuppressWarnings("unchecked")
                    List<BookSearchResponse> books = (List<BookSearchResponse>) result;
                    for (BookSearchResponse book : books) {
                        // AI가 추천 사유를 적지 않은 도서는 스킵
                        if (book.getAiComment() == null || book.getAiComment().isBlank() || "-".equals(book.getAiComment())) {
                            continue;
                        }

                        StringBuilder sb = new StringBuilder();

                        if(book.getPersonalizationScore() != null) {
                            sb.append("📊 사용자 선호도가 반영되었습니다.\n");
                        }
                        // 책 이미지 URL이 있으면 보이지 않는 링크로 프리뷰 유도
                        if (book.getImageUrl() != null && !book.getImageUrl().isBlank()) {
                            sb.append("[\u200B](").append(book.getImageUrl()).append(")");
                        }

                        sb.append("**").append(book.getTitle()).append("**\n");
                        sb.append("저자: ").append(book.getAuthorName() != null ? book.getAuthorName() : "-").append("\n");
                        sb.append("출판사: ").append(book.getPublisherName() != null ? book.getPublisherName() : "-").append("\n");
                        if (book.getRelevance() != null) {
                            sb.append("연관도: ").append(book.getRelevance()).append("%\n");
                        }
                        sb.append("\n추천 사유:\n");
                        sb.append(book.getAiComment());

                        responses.add(new AskResponse(book.getId(), sb.toString()));
                    }
                } else if (result instanceof String) {
                    // 대출 정보 등 일반 스트링 리포트인 경우
                    responses.add(new AskResponse(null, (String) result));
                }
            }

            log.info("[Assistant] 하이브리드 DTO 직접 조립 완료 (총 {}건)", responses.size());
            return responses;

        } catch (Exception e) {
            log.error("[Assistant] 답변 생성 중 에러 발생", e);
            return List.of(new AskResponse(null, "죄송합니다. 요청을 처리하는 중 문제가 발생했습니다."));
        }
    }
}
