package com.nhnacademy.springailibrarycore.agent;

import com.nhnacademy.springailibrarycore.telegram.dto.AskRequest;
import com.nhnacademy.springailibrarycore.telegram.dto.AskResponse;
import com.nhnacademy.springailibrarycore.telegram.dto.IntentResult;
import com.nhnacademy.springailibrarycore.telegram.exception.NotFoundIntentException;
import com.nhnacademy.springailibrarycore.telegram.handler.IntentHandler;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 텔레그램 모듈의 단일 진입점으로부터 요청을 받아 의도를 분류하고(Supervisor),
 * 각 하위 서비스/에이전트로 작업을 위임하는 라우터 에이전트.
 */
@Service
@Slf4j
public class AiLibraryAssistantAgent {

    private final ChatClient chatClient;
    private final List<IntentHandler> handlers;
    // private final LibraryInfoService libraryInfoService;
    // private final PersonalizedSearchService personalizedSearchService;

    public AiLibraryAssistantAgent(
            @Qualifier("geminiChatClientBuilder") ChatClient.Builder chatClientBuilder,
            List<IntentHandler> handlers
            // LibraryInfoService libraryInfoService,
            // PersonalizedSearchService personalizedSearchService
    ) {
        this.chatClient = chatClientBuilder.defaultSystem( """
                당신은 도서관 이용자의 질문 의도를 정확하게 파악하는 AI 라우터입니다.
                사용자의 입력 문장을 분석하여 다음 3가지 중 하나의 의도(Intent)로 분류하고, 검색에 사용할 핵심 키워드를 추출하세요.
                
                1. LIBRARY_INFO: 도서관 운영 시간, 위치, 휴관일, 이용 규칙 등 도서관 자체에 대한 질문
                2. BOOK_RECOMMENDATION: 특정 책 검색, 도서 추천, 특정 주제의 책을 찾아달라는 요청
                3. LIBRARY_BOOK_RECOMMENDATION: LIBRARY_INFO와 BOOK_RECOMMENDATION 모두 해당하는 요청
                3. GENERAL_CHAT: 인삿말 등 위 세 가지에 해당하지 않는 일상 대화
                """)
                .build();
        this.handlers = handlers;
    }



    @Transactional
    public List<AskResponse> ask(AskRequest request) {
        try {
            // 1. LLM을 이용한 의도 분류 (Structured Output)
            IntentResult intentResult = chatClient.prompt()
                    .user(request.question())
                    .call()
                    .entity(IntentResult.class);

            log.info("[Supervisor] 분류된 의도: {}, 추출된 키워드: {}", intentResult.intent(), intentResult.keyword());

            IntentHandler targetHandler = handlers.stream()
                    .filter(handler -> handler.supports(intentResult.intent()))
                    .findFirst()
                    .orElseThrow(()-> new NotFoundIntentException());
            return targetHandler.handle(request);

        } catch (Exception e) {
            log.error("[Supervisor] 의도 분류 및 라우팅 중 에러 발생", e);
            return List.of(new AskResponse(null, "죄송합니다. 요청을 처리하는 중 문제가 발생했습니다."));
        }
    }
}
