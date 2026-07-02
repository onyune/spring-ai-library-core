package com.nhnacademy.springailibrarycore.telegram.controller;

import com.nhnacademy.springailibrarycore.telegram.agent.AiLibraryAssistantAgent;
import com.nhnacademy.springailibrarycore.telegram.dto.AskRequest;
import com.nhnacademy.springailibrarycore.telegram.dto.AskResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 도서관 비서 에이전트(AiLibraryAssistantAgent) 호출을 위한 테스트 컨트롤러
 */
@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
@Slf4j
public class AiLibraryAssistantController {

    private final AiLibraryAssistantAgent aiLibraryAssistantAgent;

    /**
     * 사용자의 자연어 질문을 접수하여 AI 에이전트를 통해 처리한 후 답변을 반환합니다.
     */
    @PostMapping("/ask")
    public ResponseEntity<List<AskResponse>> ask(@RequestBody AskRequest request) {
        log.info("[AiLibraryAssistantController] AI 비서 질문 접수: {}", request.question());
        
        // Agent(Supervisor)가 의도를 분석하고 적절한 서비스로 라우팅한 뒤 List<AskResponse>로 반환합니다.
        List<AskResponse> responses = aiLibraryAssistantAgent.ask(request);

        return ResponseEntity.ok(responses);
    }

}
