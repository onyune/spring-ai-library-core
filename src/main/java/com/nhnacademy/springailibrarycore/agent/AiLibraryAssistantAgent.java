package com.nhnacademy.springailibrarycore.agent;

import com.nhnacademy.springailibrarycore.telegram.dto.AskRequest;
import com.nhnacademy.springailibrarycore.telegram.dto.AskResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 텔레그램 모듈의 단일 진입점으로부터 요청을 받아
 * Tool을 활용해 사용자에게 최종 답변을 생성하는 만능 사서 에이전트.
 */
@Service
@Slf4j
public class AiLibraryAssistantAgent {

    private final ChatClient chatClient;

    public AiLibraryAssistantAgent(
            @Qualifier("geminiChatClientBuilder") ChatClient.Builder chatClientBuilder
    ) {
        this.chatClient = chatClientBuilder.defaultSystem( """
                당신은 NHN 도서관의 친절하고 전문적인 AI 사서입니다.
                사용자의 질문에 답변하기 위해 제공된 도구(Tools)들을 적극적으로 활용하세요.
                도구에서 얻은 정보를 바탕으로 사용자에게 친절한 문장으로 답변을 작성해야 합니다.
                
                [중요: 응답 형식 규칙]
                응답은 반드시 JSON 배열 형태로 작성해야 합니다.
                만약 도구에서 특정 도서(bookId)에 대한 정보를 찾았거나 추천하는 경우, 배열의 각 항목에 해당 도서의 bookId와 답변(answer)을 분리해서 담아주세요.
                추천할 도서가 여러 권이라면 배열에 여러 항목을 만들어주세요.
                단순 도서관 안내처럼 특정 도서와 관련 없는 답변이라면 bookId는 null로 설정하세요.
                """)
                // .defaultFunctions("searchBookTool", "getLibraryInfoTool") // TODO: 여기에 사용할 Tool 이름들을 등록하세요.
                .build();
    }

    @Transactional
    public List<AskResponse> ask(AskRequest request) {
        try {
            // 1. LLM에게 질문을 던지고, 결과를 List<AskResponse> 형태로 받습니다.
            List<AskResponse> aiResponses = chatClient.prompt()
                    .user(request.question())
                    .call()
                    .entity(new ParameterizedTypeReference<List<AskResponse>>() {});

            log.info("[Assistant] 최종 생성된 응답 갯수: {}", aiResponses.size());
            aiResponses.forEach(r -> log.info(" - bookId: {}, answer: {}", r.bookId(), r.answer()));

            return aiResponses;

        } catch (Exception e) {
            log.error("[Assistant] 답변 생성 중 에러 발생", e);
            return List.of(new AskResponse(null, "죄송합니다. 요청을 처리하는 중 문제가 발생했습니다."));
        }
    }
}
