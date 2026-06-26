package com.nhnacademy.springailibrarycore.library.controller;

import com.nhnacademy.springailibrarycore.agent.AiLibraryAssistantAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AskResponse> ask(@RequestBody AskRequest request) {
        log.info("[AiLibraryAssistantController] AI 비서 질문 접수: {}", request.message());
        String answer = aiLibraryAssistantAgent.ask(request.message());
        return ResponseEntity.ok(new AskResponse(answer));
    }

    public record AskRequest(String message) {}
    public record AskResponse(String answer) {}
}
