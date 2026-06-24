package com.nhnacademy.springailibrarycore.book.strategy.impl;

import com.nhnacademy.springailibrarycore.book.domain.SearchType;
import com.nhnacademy.springailibrarycore.book.dto.BookSearchRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Service;

/**
 * AUTO는 ChatClient가 사용자의 질문에 맞춰
 * Keyword 전략을 쓸지, RAG 전략을 쓸 지 결정하여 해당 전략을 실행시킵니다.
 */
@Slf4j
@Service
public class AutoSearchAgent {
    private final ChatClient chatClient;

    public AutoSearchAgent(@Qualifier("ollamaChatClientBuilder") ChatClient.Builder chatClient) {
        String systemPrompt = """
                너는 도서관의 스마트 검색 라우팅 어시스턴트야.
                너는 스마트 도서 검색 라우터야.
                [규칙]
                명사형 키워드면 strategy를 "KEYWORD"로, 문장형/추천 요청이면 "RAG"로 설정해.
                KEYWORD일땐 parsedQuery에는 조사/기호를 제거한 핵심 검색어만 담고 RAG는 키워드 그대로 담아.
                """;
        this.chatClient = chatClient
                .defaultSystem(systemPrompt)
                .build();
    }

    public SearchIntent search(BookSearchRequest request) {
        return chatClient.prompt()
                .user(request.keyword())
                .call()
                .entity(SearchIntent.class);
    }

    public record SearchIntent(@Description(value = "명사형 키워드면 KEYWORD로, 문장형 및 추천 요청이면 RAG로 설정")
                               SearchType searchType,
                               @Description(value = "searchType이 KEYWORD일땐 조사/기호를 제거한 핵심 검색어만, RAG일 경우는 키워드 그대로")
                               String parsedQuery){}
}
